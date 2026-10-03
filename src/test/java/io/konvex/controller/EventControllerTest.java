package io.konvex.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.konvex.config.MatchingProperties;
import io.konvex.engine.CorrelationEngine;
import io.konvex.engine.EventWindow;
import io.konvex.model.Event;
import io.konvex.service.MatchingService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EventControllerTest {

	@Test
	void returnsCorrelationResultsAfterAcceptingEvent() {
		MatchingProperties properties = new MatchingProperties();
		CorrelationEngine engine = new CorrelationEngine(
				new MatchingService(properties),
				properties,
				new EventWindow());
		EventController controller = new EventController(engine);

		Event first = new Event(
				"camera-a",
				"evt-1",
				28.6129,
				77.2295,
				Instant.now(),
				Map.of());
		Event second = new Event(
				"camera-b",
				"evt-2",
				28.6135,
				77.2302,
				first.timestamp().plusSeconds(10),
				Map.of());

		ResponseEntity<EventIngestResponse> firstResponse = controller.ingestEvent(first);
		ResponseEntity<EventIngestResponse> secondResponse = controller.ingestEvent(second);

		assertEquals(HttpStatus.ACCEPTED, firstResponse.getStatusCode());
		assertEquals(HttpStatus.ACCEPTED, secondResponse.getStatusCode());
		assertEquals("evt-2", secondResponse.getBody().eventId());
		assertEquals(1, secondResponse.getBody().matchCount());
	}
}
