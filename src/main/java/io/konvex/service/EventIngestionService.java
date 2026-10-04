package io.konvex.service;

import io.konvex.engine.CorrelationEngine;
import io.konvex.model.CorrelationMatch;
import io.konvex.model.Event;
import io.konvex.persistence.EventPersistenceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventIngestionService {
	private final CorrelationEngine correlationEngine;
	private final EventPersistenceService eventPersistenceService;

	public List<CorrelationMatch> ingest(Event event) {
		List<CorrelationMatch> matches = correlationEngine.processEvent(event);
		eventPersistenceService.persist(event, matches);
		return matches;
	}
}