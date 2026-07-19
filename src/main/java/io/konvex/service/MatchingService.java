package io.konvex.service;

import io.konvex.config.MatchingProperties;
import io.konvex.model.Event;
import io.konvex.util.GeoUtils;
import java.time.Duration;
import org.springframework.stereotype.Service;

/**
 * Decides whether two {@link Event}s refer to the same real-world occurrence
 * based on proximity in space and time.
 */
@Service
public class MatchingService {

	private final MatchingProperties matchingProperties;

	public MatchingService(MatchingProperties matchingProperties) {
		this.matchingProperties = matchingProperties;
	}

	/**
	 * Returns {@code true} when {@code a} and {@code b} are close enough in both
	 * space and time to be treated as a match.
	 *
	 * <p>Matching rule: both of the following must hold
	 * <ul>
	 *   <li>Haversine distance between the events' coordinates is
	 *       {@code ≤} {@link MatchingProperties#getMaxDistanceKm()}</li>
	 *   <li>Absolute time gap between the events' timestamps is
	 *       {@code ≤} {@link MatchingProperties#getMaxTimeGapSeconds()}</li>
	 * </ul>
	 * Argument order does not matter; distance and time gap are symmetric.
	 *
	 * @param a first event
	 * @param b second event
	 * @return {@code true} if both thresholds are satisfied; {@code false} otherwise
	 */
	public boolean isMatch(Event a, Event b) {
		double distanceKm = GeoUtils.haversineDistanceKm(
				a.latitude(), a.longitude(), b.latitude(), b.longitude());
		long timeGapSeconds = Math.abs(Duration.between(a.timestamp(), b.timestamp()).getSeconds());

		return distanceKm <= matchingProperties.getMaxDistanceKm()
				&& timeGapSeconds <= matchingProperties.getMaxTimeGapSeconds();
	}
}
