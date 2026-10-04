package io.konvex.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.konvex.controller.PageResponse;
import io.konvex.persistence.entity.CorrelationMatchEntity;
import io.konvex.persistence.repository.CorrelationMatchRepository;
import io.konvex.persistence.repository.EventRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class HistoryServiceTest {

	@Test
	void validatesRangeAndPagingAndBuildsMatchPage() {
		CorrelationMatchRepository matchRepository = mock(CorrelationMatchRepository.class);
		EventRepository eventRepository = mock(EventRepository.class);
		HistoryService service = new HistoryService(matchRepository, eventRepository, new ObjectMapper());

		CorrelationMatchEntity entity = CorrelationMatchEntity.builder()
				.id(7L).eventId("evt-2").source("camera-b").matchedEventId("evt-1").matchedSource("camera-a")
				.distanceKm(1.2).timeGapSeconds(9).detectedAt(Instant.parse("2026-03-15T10:30:09Z")).build();
		when(matchRepository.findAll(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(entity), Pageable.ofSize(10), 1));

		PageResponse<?> response = service.findMatches(
				"2026-03-15T10:30:00Z",
				"2026-03-15T11:00:00Z",
				"camera-b",
				0,
				10);

		assertEquals(1, response.content().size());
		assertEquals(1, response.totalElements());
	}

	@Test
	void rejectsInvalidHistoryParameters() {
		HistoryService service = new HistoryService(
				mock(CorrelationMatchRepository.class), mock(EventRepository.class), new ObjectMapper());

		assertThrows(ResponseStatusException.class, () -> service.findMatches("bad", null, null, 0, 20));
		assertThrows(ResponseStatusException.class, () -> service.findMatches("2026-03-15T11:00:00Z", "2026-03-03T10:00:00Z", null, 0, 20));
		assertThrows(ResponseStatusException.class, () -> service.findMatches(null, null, null, -1, 20));
		assertThrows(ResponseStatusException.class, () -> service.findMatches(null, null, null, 0, 101));
	}
}