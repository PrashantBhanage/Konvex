package io.konvex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "konvex.matching")
public class MatchingProperties {

	/**
	 * Maximum distance in kilometres between two events for them to be considered a match.
	 */
	private double maxDistanceKm = 5.0;

	/**
	 * Maximum time gap in seconds between two events for them to be considered a match.
	 */
	private long maxTimeGapSeconds = 60;

	public double getMaxDistanceKm() {
		return maxDistanceKm;
	}

	public void setMaxDistanceKm(double maxDistanceKm) {
		this.maxDistanceKm = maxDistanceKm;
	}

	public long getMaxTimeGapSeconds() {
		return maxTimeGapSeconds;
	}

	public void setMaxTimeGapSeconds(long maxTimeGapSeconds) {
		this.maxTimeGapSeconds = maxTimeGapSeconds;
	}
}
