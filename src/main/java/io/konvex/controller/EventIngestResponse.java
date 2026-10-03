package io.konvex.controller;

import io.konvex.model.CorrelationMatch;
import java.util.List;

/**
 * Response returned after an event is ingested and correlated.
 */
public record EventIngestResponse(
        String status,
        String eventId,
        int matchCount,
        List<CorrelationMatch> matches) {
}
