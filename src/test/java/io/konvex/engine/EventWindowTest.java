package io.konvex.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.konvex.model.Event;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
