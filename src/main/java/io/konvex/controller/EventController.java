package io.konvex.controller;

import io.konvex.model.CorrelationMatch;
import io.konvex.model.Event;
import io.konvex.service.EventIngestionService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {
	private final EventIngestionService eventIngestionService;
	public EventController(EventIngestionService eventIngestionService) { this.eventIngestionService = eventIngestionService; }

	@PostMapping
	public ResponseEntity<EventIngestResponse> ingestEvent(@RequestBody Event event) {
		List<CorrelationMatch> matches = eventIngestionService.ingest(event);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(
				new EventIngestResponse("accepted", event.eventId(), matches.size(), matches));
	}
}