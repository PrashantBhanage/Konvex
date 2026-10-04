package io.konvex.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.konvex.model.CorrelationMatch;
import io.konvex.model.Event;
import io.konvex.service.EventIngestionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EventControllerTest {

	@Test
	void returnsCorrelationResultsAfterAcceptingEvent() {
		EventIngestionService ingestionService = mock(EventIngestionService.class);
		EventController controller = new EventController(ingestionService);

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

		when(ingestionService.ingest(first)).thenReturn(List.of());
		CorrelationMatch match = new CorrelationMatch(
				"evt-2",
				"camera-b",
				"evt-1",
				"camera-a",
				0.1,
				10,
				Instant.now());
		when(ingestionService.ingest(second)).thenReturn(List.of(match));

		ResponseEntity<EventIngestResponse> firstResponse = controller.ingestEvent(first);
		ResponseEntity<EventIngestResponse> secondResponse = controller.ingestEvent(second);

		assertEquals(HttpStatus.ACCEPTED, firstResponse.getStatusCode());
		assertEquals(HttpStatus.ACCEPTED, secondResponse.getStatusCode());
		assertEquals("evt-2", secondResponse.getBody().eventId());
		assertEquals(1, secondResponse.getBody().matchCount());
		verify(ingestionService).ingest(first);
		verify(ingestionService).ingest(second);
	}
}
