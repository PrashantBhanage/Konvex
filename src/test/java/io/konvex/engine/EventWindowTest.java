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
