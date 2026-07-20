package io.konvex.controller;

import io.konvex.engine.CorrelationEngine;
import io.konvex.model.Event;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

	private final CorrelationEngine correlationEngine;

	public EventController(CorrelationEngine correlationEngine) {
		this.correlationEngine = correlationEngine;
	}

	@PostMapping
	public ResponseEntity<Map<String, String>> ingestEvent(@RequestBody Event event) {
		correlationEngine.processEvent(event);
		return ResponseEntity
				.status(HttpStatus.ACCEPTED)
				.body(Map.of(
						"status", "accepted",
						"eventId", event.eventId()));
	}
}
