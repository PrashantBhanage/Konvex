package io.konvex.engine;

import io.konvex.config.MatchingProperties;
import io.konvex.model.Event;
import io.konvex.service.MatchingService;
import io.konvex.util.GeoUtils;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Streaming correlation engine: compares each incoming event against a recent
 * window and reports spatial/temporal matches.
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
	 * Processes a single event: evicts stale window entries, compares against
	 * remaining events, logs matches (or lack thereof), then adds the event
	 * to the window.
	 */
	public void processEvent(Event newEvent) {
		eventWindow.evictExpired(
				newEvent.timestamp(),
				matchingProperties.getMaxTimeGapSeconds());

		boolean anyMatch = false;
		for (Event existing : eventWindow.getRecentEvents()) {
			if (matchingService.isMatch(newEvent, existing)) {
				anyMatch = true;
				double distanceKm = GeoUtils.haversineDistanceKm(
						newEvent.latitude(),
						newEvent.longitude(),
						existing.latitude(),
						existing.longitude());
				long timeGapSeconds = Math.abs(
						Duration.between(newEvent.timestamp(), existing.timestamp()).getSeconds());

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

		if (!anyMatch) {
			log.info(
					"NEW unmatched event | id={} source={} lat={} lon={} at={}",
					newEvent.eventId(),
					newEvent.source(),
					newEvent.latitude(),
					newEvent.longitude(),
					newEvent.timestamp());
		}
	}
}
