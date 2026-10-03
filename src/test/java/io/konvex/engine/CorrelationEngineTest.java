package io.konvex.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.konvex.config.MatchingProperties;
import io.konvex.model.CorrelationMatch;
import io.konvex.model.Event;
import io.konvex.service.MatchingService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CorrelationEngineTest {

	private CorrelationEngine correlationEngine;
	private Instant baseTime;

	@BeforeEach
	void setUp() {
		MatchingProperties properties = new MatchingProperties();
		correlationEngine = new CorrelationEngine(
				new MatchingService(properties),
				properties,
				new EventWindow());
		baseTime = Instant.now();
	}

	@Test
	void returnsMatchingEvents() {
		Event first = event("camera-a", "evt-1", 28.6129, 77.2295, baseTime);
		Event second = event("camera-b", "evt-2", 28.6135, 77.2302, baseTime.plusSeconds(15));

		assertTrue(correlationEngine.processEvent(first).isEmpty());

		List<CorrelationMatch> matches = correlationEngine.processEvent(second);

		assertEquals(1, matches.size());
		assertEquals("evt-2", matches.get(0).eventId());
		assertEquals("evt-1", matches.get(0).matchedEventId());
		assertEquals("camera-a", matches.get(0).matchedSource());
	}

	@Test
	void doesNotMatchEventsOutsideTimeWindow() {
		Event first = event("camera-a", "evt-1", 28.6129, 77.2295, baseTime);
		Event second = event("camera-b", "evt-2", 28.6135, 77.2302, baseTime.plusSeconds(61));

		correlationEngine.processEvent(first);

		assertTrue(correlationEngine.processEvent(second).isEmpty());
	}

	@Test
	void doesNotCorrelateTheSameObservation() {
		Event first = event("OpenSky", "aircraft-1", 28.6129, 77.2295, baseTime);
		Event repeated = event("OpenSky", "aircraft-1", 28.6130, 77.2296, baseTime.plusSeconds(10));

		correlationEngine.processEvent(first);

		assertTrue(correlationEngine.processEvent(repeated).isEmpty());
	}

	@Test
	void canReturnMultipleMatches() {
		Event first = event("camera-a", "evt-1", 28.6129, 77.2295, baseTime);
		Event second = event("sensor-b", "evt-2", 28.6132, 77.2298, baseTime.plusSeconds(5));
		Event third = event("sensor-c", "evt-3", 28.6133, 77.2299, baseTime.plusSeconds(10));

		correlationEngine.processEvent(first);
		correlationEngine.processEvent(second);

		List<CorrelationMatch> matches = correlationEngine.processEvent(third);

		assertEquals(2, matches.size());
	}

	private static Event event(
			String source,
			String eventId,
			double latitude,
			double longitude,
			Instant timestamp) {
		return new Event(source, eventId, latitude, longitude, timestamp, Map.of());
	}
}
