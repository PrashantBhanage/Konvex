package io.konvex.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.konvex.model.CorrelationMatch;
import io.konvex.model.Event;
import io.konvex.persistence.entity.CorrelationMatchEntity;
import io.konvex.persistence.repository.CorrelationMatchRepository;
import io.konvex.persistence.repository.EventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class EventPersistenceServiceTest {

	@Test
	void persistsEventMetadataAndMatches() {
		EventRepository eventRepository = mock(EventRepository.class);
		CorrelationMatchRepository matchRepository = mock(CorrelationMatchRepository.class);
		EventPersistenceService service = new EventPersistenceService(eventRepository, matchRepository, new ObjectMapper());

		Event event = new Event("camera-a", "evt-1", 28.6129, 77.2295,
				Instant.parse("2026-03-15T10:30:00Z"), Map.of("type", "vehicle"));
		CorrelationMatch match = new CorrelationMatch("evt-2", "camera-b", "evt-1", "camera-a",
				0.75, 15, Instant.parse("2026-03-15T10:30:15Z"));

		service.persist(event, List.of(match));

		ArgumentCaptor<io.konvex.persistence.entity.EventEntity> eventCaptor =
				ArgumentCaptor.forClass(io.konvex.persistence.entity.EventEntity.class);
		verify(eventRepository).save(eventCaptor.capture());
		EventEntity savedEvent = eventCaptor.getValue();
		assertEquals("camera-a", savedEvent.getSource());
		assertEquals("evt-1", savedEvent.getEventId());
		assertEquals(28.6129, savedEvent.getLatitude());
		assertEquals("{\"type\":\"vehicle\"}", savedEvent.getMetadataJson());

		ArgumentCaptor<List<CorrelationMatchEntity>> matchCaptor = ArgumentCaptor.forClass(List.class);
		verify(matchRepository).saveAll(matchCaptor.capture());
		List<CorrelationMatchEntity> savedMatches = matchCaptor.getValue();
		assertNotNull(savedMatches);
		assertEquals(1, savedMatches.size());
		assertEquals("evt-2", savedMatches.get(0).getEventId());
		assertEquals("camera-a", savedMatches.get(0).getMatchedSource());
		assertEquals(0.75, savedMatches.get(0).getDistanceKm());
		assertEquals(15, savedMatches.get(0).getTimeGapSeconds());
	}

	@Test
	void persistsEventWhenThereAreNoMatches() {
		EventRepository eventRepository = mock(EventRepository.class);
		CorrelationMatchRepository matchRepository = mock(CorrelationMatchRepository.class);
		EventPersistenceService service = new EventPersistenceService(eventRepository, matchRepository, new ObjectMapper());
		Event event = new Event("OpenSky", "abc123", 51.5, -0.12,
				Instant.parse("2026-03-15T10:30:00Z"), Map.of());

		service.persist(event, List.of());

		verify(eventRepository).save(any(EventEntity.class));
	}
}