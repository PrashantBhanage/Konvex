package io.konvex.model;

import java.time.Instant;

/**
 * Describes a pair of events that were correlated by Konvex.
 */
public record CorrelationMatch(
        String eventId,
        String source,
        String matchedEventId,
        String matchedSource,
        double distanceKm,
        long timeGapSeconds,
        Instant detectedAt) {
}
