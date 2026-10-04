package io.konvex.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.konvex.model.Event;
import io.konvex.util.GeoUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class EventWindowTest {

	@Test
	void replacesOlderObservationWithSameSourceAndId() {
		EventWindow window = new EventWindow();
		Instant now = Instant.now();

		window.add(event("OpenSky", "abc123", 28.1, 77.1, now.minusSeconds(5)));
		window.add(event("OpenSky", "abc123", 28.2, 77.2, now));

		List<Event> events = toList(window.getRecentEvents());

		assertEquals(1, events.size());
		assertEquals(28.2, events.get(0).latitude());
		assertEquals(77.2, events.get(0).longitude());
	}

	@Test
	void keepsEventsWithSameIdFromDifferentSources() {
		EventWindow window = new EventWindow();
		Instant now = Instant.now();

		window.add(event("camera-a", "same-id", 28.1, 77.1, now));
		window.add(event("camera-b", "same-id", 28.2, 77.2, now));

		assertEquals(2, toList(window.getRecentEvents()).size());
	}

	@Test
	void returnsNearbyCandidatesAndSkipsDistantBuckets() {
		EventWindow window = new EventWindow();
		Instant now = Instant.now();
		Event reference = event("camera-a", "reference", 28.6129, 77.2295, now);

		window.add(event("camera-b", "nearby", 28.6135, 77.2302, now));
		window.add(event("camera-c", "far-away", 28.6129, 77.7400, now));

		List<Event> candidates = window.getNearbyEvents(reference, 5.0);

		assertEquals(1, candidates.size());
		assertEquals("nearby", candidates.get(0).eventId());
	}

	@Test
	void evictsEventsOutsideEventTimeWindow() {
		EventWindow window = new EventWindow();
		Instant referenceTime = Instant.parse("2026-03-15T10:31:00Z");

		window.add(event(
				"camera-a",
				"old",
				28.1,
				77.1,
				Instant.parse("2026-03-15T10:29:59Z")));
		window.add(event(
				"camera-b",
				"fresh",
				28.1,
				77.1,
				Instant.parse("2026-03-15T10:30:10Z")));

		window.evictExpired(referenceTime, 60);

		assertEquals(1, toList(window.getRecentEvents()).size());
		assertEquals("fresh", toList(window.getRecentEvents()).get(0).eventId());
	}

	@Test
	void remainsConsistentWhenManyThreadsIngestConcurrentEvents() throws Exception {
		EventWindow window = new EventWindow();
		Instant baseTime = Instant.parse("2026-03-15T10:30:00Z");

		int workerCount = 8;
		int uniqueEventsPerWorker = 100;
		int sharedUpdatesPerWorker = 50;

		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();

		try {
			for (int worker = 0; worker < workerCount; worker++) {
				final int workerId = worker;
				futures.add(executor.submit(() -> {
					start.await();

					for (int i = 0; i < uniqueEventsPerWorker; i++) {
						window.add(event(
								"worker-" + workerId,
								"event-" + i,
								10.0 + workerId,
								70.0 + (i * 0.01),
								baseTime.plusNanos(i)));
					}

					for (int i = 0; i < sharedUpdatesPerWorker; i++) {
						window.add(event(
								"shared",
								"shared-event",
								0.0,
								0.0,
								baseTime.plusNanos(i)));
					}

					return null;
				}));
			}

			start.countDown();

			for (Future<?> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		} finally {
			executor.shutdownNow();
		}

		List<Event> recent = toList(window.getRecentEvents());
		Set<String> identities = new HashSet<>();
		for (Event event : recent) {
			identities.add(event.source() + '\u0000' + event.eventId());
		}

		assertEquals(
				workerCount * uniqueEventsPerWorker + 1,
				recent.size());
		assertEquals(recent.size(), identities.size());

		List<Event> sharedCandidates = window.getNearbyEvents(
				event("probe", "probe", 0.0, 0.0, baseTime),
				5.0);

		assertEquals(1, sharedCandidates.size());
		assertEquals("shared-event", sharedCandidates.get(0).eventId());
	}

	@Test
	void returnsCandidatesAtHighLatitude() {
		EventWindow window = new EventWindow();
		Instant now = Instant.parse("2026-03-15T10:30:00Z");
		Event reference = event("reference", "ref", 70.0, 10.09, now);
		Event nearby = event(
				"aircraft",
				"high-latitude",
				69.9755,
				10.2001,
				now.plusSeconds(10));

		window.add(nearby);

		double distanceKm = GeoUtils.haversineDistanceKm(
				reference.latitude(),
				reference.longitude(),
				nearby.latitude(),
				nearby.longitude());

		assertTrue(distanceKm <= 5.0);
		assertEquals(1, window.getNearbyEvents(reference, 5.0).size());
		assertEquals("high-latitude", window.getNearbyEvents(reference, 5.0).get(0).eventId());
	}

	@Test
	void wrapsAcrossAntimeridian() {
		EventWindow window = new EventWindow();
		Instant now = Instant.parse("2026-03-15T10:30:00Z");
		Event reference = event("reference", "ref", 0.0, 179.98, now);
		Event nearby = event("sensor", "across-180", 0.0, -179.99, now);

		double distanceKm = GeoUtils.haversineDistanceKm(
				reference.latitude(),
				reference.longitude(),
				nearby.latitude(),
				nearby.longitude());

		window.add(nearby);

		assertTrue(distanceKm <= 5.0);
		assertEquals(1, window.getNearbyEvents(reference, 5.0).size());
		assertEquals("across-180", window.getNearbyEvents(reference, 5.0).get(0).eventId());
	}

	@Test
	void coversAllLongitudesWhenSearchRadiusReachesPole() {
		EventWindow window = new EventWindow();
		Instant now = Instant.parse("2026-03-15T10:30:00Z");
		Event reference = event("reference", "ref", 89.95, 0.0, now);
		Event nearby = event("aircraft", "near-pole", 89.99, 170.0, now);

		double distanceKm = GeoUtils.haversineDistanceKm(
				reference.latitude(),
				reference.longitude(),
				nearby.latitude(),
				nearby.longitude());

		window.add(nearby);

		assertTrue(distanceKm <= 10.0);
		assertEquals(1, window.getNearbyEvents(reference, 10.0).size());
		assertEquals("near-pole", window.getNearbyEvents(reference, 10.0).get(0).eventId());
	}

	private static Event event(
			String source,
			String eventId,
			double latitude,
			double longitude,
			Instant timestamp) {
		return new Event(source, eventId, latitude, longitude, timestamp, Map.of());
	}

	private static List<Event> toList(Iterable<Event> events) {
		List<Event> result = new ArrayList<>();
		for (Event event : events) {
			result.add(event);
		}
		return result;
	}
}
