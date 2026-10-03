package io.konvex.engine;

import io.konvex.model.Event;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Thread-safe sliding window of recently seen events for stream correlation.
 *
 * <p>The window also keeps a coarse geographic index so the correlation engine
 * can inspect nearby events without scanning the entire stream.
 */
@Component
public class EventWindow {

	private static final double LAT_CELL_DEGREES = 0.1;
	private static final double LON_CELL_DEGREES = 0.1;
	private static final int LON_BUCKET_COUNT = 3600;

	private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<>();
	private final ConcurrentMap<EventKey, Event> latestByIdentity = new ConcurrentHashMap<>();
	private final ConcurrentMap<GeoBucket, Set<Event>> buckets = new ConcurrentHashMap<>();

	/**
	 * Returns a live view of all events currently retained by the window.
	 */
	public Iterable<Event> getRecentEvents() {
		return events;
	}

	/**
	 * Returns events from geographic buckets that could contain a match within
	 * {@code maxDistanceKm}. The caller still performs the exact Haversine check.
	 */
	public List<Event> getNearbyEvents(Event reference, double maxDistanceKm) {
		int centerLatBucket = latBucket(reference.latitude());
		int centerLonBucket = lonBucket(reference.longitude());

		int latRadius = Math.max(
				1,
				(int) Math.ceil(
						maxDistanceKm / (111.32 * LAT_CELL_DEGREES)));

		double latitudeRadians = Math.toRadians(reference.latitude());
		double kmPerLongitudeDegree = 111.32 * Math.max(Math.cos(latitudeRadians), 0.01);
		int lonRadius = Math.max(
				1,
				(int) Math.ceil(
						maxDistanceKm / (kmPerLongitudeDegree * LON_CELL_DEGREES)));

		List<Event> candidates = new ArrayList<>();
		for (int latOffset = -latRadius; latOffset <= latRadius; latOffset++) {
			int lat = centerLatBucket + latOffset;
			for (int lonOffset = -lonRadius; lonOffset <= lonRadius; lonOffset++) {
				int lon = Math.floorMod(centerLonBucket + lonOffset, LON_BUCKET_COUNT);
				Set<Event> bucketEvents = buckets.get(new GeoBucket(lat, lon));
				if (bucketEvents != null) {
					candidates.addAll(bucketEvents);
				}
			}
		}

		return List.copyOf(candidates);
	}

	/**
	 * Adds a newly seen event and replaces any older observation with the same
	 * source and event ID.
	 */
	public void add(Event event) {
		EventKey key = new EventKey(event.source(), event.eventId());
		Event previous = latestByIdentity.put(key, event);

		if (previous != null) {
			removeFromBucket(previous);
		}

		events.add(event);
		buckets
				.computeIfAbsent(bucketFor(event), ignored -> ConcurrentHashMap.newKeySet())
				.add(event);
	}

	/**
	 * Removes observations that fall outside the configured event-time window.
	 */
	public void evictExpired(Instant referenceTime, long maxTimeGapSeconds) {
		Instant cutoff = referenceTime.minusSeconds(maxTimeGapSeconds);

		for (Event event : events) {
			if (event.timestamp().isBefore(cutoff) && events.remove(event)) {
				removeFromBucket(event);
				EventKey key = new EventKey(event.source(), event.eventId());
				latestByIdentity.remove(key, event);
			}
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
			buckets.remove(bucket, bucketEvents);
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
