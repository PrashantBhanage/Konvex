package io.konvex.controller;

import io.konvex.service.HistoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HistoryController {

	private final HistoryService historyService;

	@GetMapping("/matches")
	public ResponseEntity<PageResponse<CorrelationMatchResponse>> getMatches(
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to,
			@RequestParam(required = false) String source,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(historyService.findMatches(from, to, source, page, size));
	}

	@GetMapping("/events/{source}/{eventId}")
	public ResponseEntity<List<EventHistoryResponse>> getEventHistory(
			@PathVariable String source, @PathVariable String eventId) {
		return ResponseEntity.ok(historyService.findEvents(source, eventId));
	}
}