package io.konvex.controller;

import io.konvex.persistence.entity.EventEntity;
import java.time.Instant;
import java.util.Map;

public record EventHistoryResponse(
		Long id,
		String source,
		String eventId,
		double latitude,
		double longitude,
		Instant timestamp,
		Map<String, Object> metadata) {

	public static EventHistoryResponse from(EventEntity entity, Map<String, Object> metadata) {
		return new EventHistoryResponse(
				entity.getId(), entity.getSource(), entity.getEventId(), entity.getLatitude(), entity.getLongitude(),
				entity.getEventTimestamp(), metadata);
	}
}