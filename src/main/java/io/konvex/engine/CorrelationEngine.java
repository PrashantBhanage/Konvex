package io.konvex.engine;

import io.konvex.config.MatchingProperties;
import io.konvex.model.CorrelationMatch;
import io.konvex.model.Event;
import io.konvex.service.MatchingService;
import io.konvex.util.GeoUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Streaming correlation engine: compares each incoming event against a recent
 * event-time window and reports spatial/temporal matches.
 */
@Component
public class CorrelationEngine {

	private static final Logger log = LoggerFactory.getLogger(CorrelationEngine.class);

	private final MatchingService matchingService;
	private final MatchingProperties matchingProperties;
	private final EventWindow eventWindow;

	public CorrelationEngine(
			MatchingService matchingService,
			MatchingProperties matchingProperties,
			EventWindow eventWindow) {
		this.matchingService = matchingService;
		this.matchingProperties = matchingProperties;
		this.eventWindow = eventWindow;
	}

	/**
	 * Processes a single event and returns all recent events that satisfy the
	 * configured spatial and temporal matching thresholds.
	 */
	public List<CorrelationMatch> processEvent(Event newEvent) {
		eventWindow.evictExpired(
				newEvent.timestamp(),
				matchingProperties.getMaxTimeGapSeconds());

		List<CorrelationMatch> matches = new ArrayList<>();
		Instant detectedAt = Instant.now();

		for (Event existing : eventWindow.getRecentEvents()) {
			if (isSameObservation(existing, newEvent)) {
				continue;
			}

			if (matchingService.isMatch(newEvent, existing)) {
				double distanceKm = GeoUtils.haversineDistanceKm(
						newEvent.latitude(),
						newEvent.longitude(),
						existing.latitude(),
						existing.longitude());
				long timeGapSeconds = Math.abs(
						Duration.between(newEvent.timestamp(), existing.timestamp()).getSeconds());

				CorrelationMatch match = new CorrelationMatch(
						newEvent.eventId(),
						newEvent.source(),
						existing.eventId(),
						existing.source(),
						distanceKm,
						timeGapSeconds,
						detectedAt);

				matches.add(match);

				log.info(
						"MATCHED | new={} ({}) <-> existing={} ({}) | distance={} km | timeGap={} s",
						newEvent.eventId(),
						newEvent.source(),
						existing.eventId(),
						existing.source(),
						String.format("%.3f", distanceKm),
						timeGapSeconds);
			}
		}

		eventWindow.add(newEvent);

		if (matches.isEmpty()) {
			log.info(
						"NEW unmatched event | id={} source={} lat={} lon={} at={}",
						newEvent.eventId(),
						newEvent.source(),
						newEvent.latitude(),
						newEvent.longitude(),
						newEvent.timestamp());
		}

		return List.copyOf(matches);
	}

	private static boolean isSameObservation(Event first, Event second) {
		return first.source().equals(second.source())
				&& first.eventId().equals(second.eventId());
	}
}
