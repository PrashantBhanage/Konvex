package io.konvex.controller;

import io.konvex.model.CorrelationMatch;
import io.konvex.persistence.entity.CorrelationMatchEntity;
import java.time.Instant;

public record CorrelationMatchResponse(
		Long id,
		String eventId,
		String source,
		String matchedEventId,
		String matchedSource,
		double distanceKm,
		long timeGapSeconds,
		Instant detectedAt) {

	public static CorrelationMatchResponse from(CorrelationMatchEntity entity) {
		return new CorrelationMatchResponse(
				entity.getId(), entity.getEventId(), entity.getSource(), entity.getMatchedEventId(),
				entity.getMatchedSource(), entity.getDistanceKm(), entity.getTimeGapSeconds(), entity.getDetectedAt());
	}
}