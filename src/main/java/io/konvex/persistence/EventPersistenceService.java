package io.konvex.persistence;

import io.konvex.model.CorrelationMatch;
import io.konvex.model.Event;
import io.konvex.persistence.entity.CorrelationMatchEntity;
import io.konvex.persistence.entity.EventEntity;
import io.konvex.persistence.repository.CorrelationMatchRepository;
import io.konvex.persistence.repository.EventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class EventPersistenceService {
	private final EventRepository eventRepository;
	private final CorrelationMatchRepository correlationMatchRepository;
	private final ObjectMapper objectMapper;

	@Transactional
	public void persist(Event event, List<CorrelationMatch> matches) {
		eventRepository.save(EventEntity.from(event, serializeMetadata(event)));
		if (matches != null && !matches.isEmpty()) {
			correlationMatchRepository.saveAll(matches.stream().map(CorrelationMatchEntity::from).toList());
		}
	}

	private String serializeMetadata(Event event) {
		try {
			return objectMapper.writeValueAsString(event.metadata());
		} catch (JacksonException ex) {
			throw new IllegalStateException("Failed to serialize event metadata", ex);
		}
	}
}