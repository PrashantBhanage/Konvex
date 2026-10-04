package io.konvex.engine;

import io.konvex.model.Event;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Thread-safe sliding window of recently seen events for stream correlation.
 *
 * <p>All state changes and multi-structure reads are protected by the instance
 * monitor. A simple monitor is intentional here because an event update must
 * stay atomic across the timestamp queue, identity index, and geographic index.
 */
@Component
public class EventWindow implements EventStore {

	private static final double EARTH_RADIUS_KM = 6371.0;
	private static final double LAT_CELL_DEGREES = 0.1;
	private static final double LON_CELL_DEGREES = 0.1;
	private static final int LON_BUCKET_COUNT = 3600;

	private final PriorityQueue<Event> events = new PriorityQueue<>(
			128,
			(eventA, eventB) -> eventA.timestamp().compareTo(eventB.timestamp()));
	private final Map<EventKey, Event> latestByIdentity = new HashMap<>();
	private final Map<GeoBucket, Set<Event>> buckets = new HashMap<>();

	/**
	 * Highest event timestamp observed by the window. It acts as an event-time
	 * watermark and only moves forward, even when events arrive out of order.
	 */
	private Instant eventTimeWatermark = Instant.MIN;

	/**
	 * Current timestamp cutoff derived from the monotonic event-time watermark.
	 */
	private Instant retentionCutoff = Instant.MIN;

	/**
	 * Returns a stable snapshot containing the latest retained observation for
	 * each source/event ID.
	 */
	@Override
	public synchronized List<Event> getRecentEvents() {
		return List.copyOf(latestByIdentity.values());
	}

	/**
	 * Returns events from geographic buckets that could contain a match within
	 * {@code maxDistanceKm}. The caller still performs the exact Haversine check.
	 *
	 * <p>Latitude coverage is derived from the spherical angular radius. Longitude
	 * coverage uses the worst-case latitude reached by that radius, because the
	 * east-west distance represented by one degree of longitude shrinks toward
	 * the poles. When the search radius reaches a pole, every longitude can be
	 * within the spherical cap, so all longitude buckets are inspected.
	 */
	@Override
	public synchronized List<Event> getNearbyEvents(Event reference, double maxDistanceKm) {
		double radiusKm = Math.max(0.0, maxDistanceKm);
		double angularRadiusDegrees = Math.min(
				180.0,
				Math.toDegrees(radiusKm / EARTH_RADIUS_KM));

		double minLatitude = Math.max(-90.0, reference.latitude() - angularRadiusDegrees);
		double maxLatitude = Math.min(90.0, reference.latitude() + angularRadiusDegrees);

		int minLatBucket = latBucket(minLatitude);
		int maxLatBucket = latBucket(maxLatitude);

		boolean searchAllLongitudes = minLatitude <= -90.0 || maxLatitude >= 90.0;
		int centerLonBucket = lonBucket(reference.longitude());
		int lonRadius = 0;

		if (!searchAllLongitudes) {
			double worstAbsoluteLatitude = Math.max(
					Math.abs(minLatitude),
					Math.abs(maxLatitude));
			double cosLatitude = Math.cos(Math.toRadians(worstAbsoluteLatitude));

			if (cosLatitude <= 1.0e-12) {
				searchAllLongitudes = true;
			} else {
				double longitudeRadiusDegrees = Math.min(
						180.0,
						Math.toDegrees(radiusKm / (EARTH_RADIUS_KM * cosLatitude)));

				if (longitudeRadiusDegrees >= 180.0) {
					searchAllLongitudes = true;
				} else {
					lonRadius = Math.max(
							0,
							(int) Math.ceil(longitudeRadiusDegrees / LON_CELL_DEGREES));
				}
			}
		}

		List<Event> candidates = new ArrayList<>();

		for (int latBucket = minLatBucket; latBucket <= maxLatBucket; latBucket++) {
			if (searchAllLongitudes) {
				for (int lonBucket = 0; lonBucket < LON_BUCKET_COUNT; lonBucket++) {
					addBucketCandidates(candidates, latBucket, lonBucket);
				}
				continue;
			}

			for (int lonOffset = -lonRadius; lonOffset <= lonRadius; lonOffset++) {
				int lonBucket = Math.floorMod(
						centerLonBucket + lonOffset,
						LON_BUCKET_COUNT);
				addBucketCandidates(candidates, latBucket, lonBucket);
			}
		}

		return List.copyOf(candidates);
	}

	/**
	 * Adds a newly seen event and replaces any older indexed observation with
	 * the same source and event ID.
	 *
	 * <p>Out-of-order policy: correlation is evaluated before retention. An event
	 * older than the current {@link #retentionCutoff} can still match retained
	 * events if it satisfies the normal spatial/temporal rule, but it is not
	 * retained and therefore cannot move the event-time watermark backwards or
	 * replace a newer observation.
	 */
	@Override
	public synchronized void add(Event event) {
		if (event.timestamp().isBefore(retentionCutoff)) {
			return;
		}

		EventKey key = new EventKey(event.source(), event.eventId());
		Event previous = latestByIdentity.put(key, event);

		if (previous != null) {
			removeFromBucket(previous);
		}

		events.add(event);
		buckets
				.computeIfAbsent(bucketFor(event), ignored -> new HashSet<>())
				.add(event);
	}

	/**
	 * Advances the event-time watermark monotonically and removes observations
	 * older than the resulting retention cutoff.
	 *
	 * <p>Passing an older reference time never moves the cutoff backwards.
	 * This makes event-time expiry deterministic when REST and OpenSky events
	 * arrive out of order.
	 */
	@Override
	public synchronized void evictExpired(Instant referenceTime, long maxTimeGapSeconds) {
		if (referenceTime.isAfter(eventTimeWatermark)) {
			eventTimeWatermark = referenceTime;
		}

		retentionCutoff = eventTimeWatermark.minusSeconds(maxTimeGapSeconds);

		while (true) {
			Event event = events.peek();
			if (event == null || !event.timestamp().isBefore(retentionCutoff)) {
				return;
			}

			events.poll();
			removeFromBucket(event);
			EventKey key = new EventKey(event.source(), event.eventId());
			latestByIdentity.remove(key, event);
		}
	}

	private void addBucketCandidates(List<Event> candidates, int latitude, int longitude) {
		Set<Event> bucketEvents = buckets.get(new GeoBucket(latitude, longitude));
		if (bucketEvents != null) {
			candidates.addAll(bucketEvents);
		}
	}

	private void removeFromBucket(Event event) {
		GeoBucket bucket = bucketFor(event);
		Set<Event> bucketEvents = buckets.get(bucket);
		if (bucketEvents == null) {
			return;
		}

		bucketEvents.remove(event);
		if (bucketEvents.isEmpty()) {
			buckets.remove(bucket);
		}
	}

	private static GeoBucket bucketFor(Event event) {
		return new GeoBucket(latBucket(event.latitude()), lonBucket(event.longitude()));
	}

	private static int latBucket(double latitude) {
		return (int) Math.floor((latitude + 90.0) / LAT_CELL_DEGREES);
	}

	private static int lonBucket(double longitude) {
		return Math.floorMod(
				(int) Math.floor((longitude + 180.0) / LON_CELL_DEGREES),
				LON_BUCKET_COUNT);
	}

	private record EventKey(String source, String eventId) {
	}

	private record GeoBucket(int latitude, int longitude) {
	}
}
