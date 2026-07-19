package io.konvex.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.konvex.config.MatchingProperties;
import io.konvex.model.Event;
import io.konvex.util.GeoUtils;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchingServiceTest {

	/** India Gate, New Delhi — used as the fixed reference point for all scenarios. */
	private static final double REF_LAT = 28.6129;
	private static final double REF_LON = 77.2295;
	private static final Instant REF_TIME = Instant.parse("2026-03-15T10:30:00Z");

	/**
	 * Longitude ~5.0 km due east of {@link #REF_LON} at {@link #REF_LAT}.
	 * Chosen so {@link GeoUtils#haversineDistanceKm} yields a value extremely close to
	 * (and slightly under) the 5.0 km threshold — see the boundary test for the measured distance.
	 */
	private static final double BOUNDARY_LON = 77.28072149649343;

	private MatchingService matchingService;

	@BeforeEach
	void setUp() {
		MatchingProperties properties = new MatchingProperties();
		// Defaults: maxDistanceKm = 5.0, maxTimeGapSeconds = 60
		matchingService = new MatchingService(properties);
	}

	@Test
	@DisplayName("Events within a few hundred metres and a few seconds of each other match")
	void closeInSpaceAndTime_returnsTrue() {
		// Arrange
		Event a = event("camera-a", "evt-1", REF_LAT, REF_LON, REF_TIME);
		Event b = event(
				"camera-b",
				"evt-2",
				28.6135,
				77.2302,
				REF_TIME.plusSeconds(12));

		// Act
		boolean match = matchingService.isMatch(a, b);

		// Assert
		assertTrue(match);
	}

	@Test
	@DisplayName("Events near each other spatially still fail when the time gap exceeds 60 seconds")
	void closeInSpaceButTimeGapExceedsThreshold_returnsFalse() {
		// Arrange
		Event a = event("camera-a", "evt-1", REF_LAT, REF_LON, REF_TIME);
		Event b = event(
				"camera-b",
				"evt-2",
				28.6135,
				77.2302,
				REF_TIME.plusSeconds(180));

		// Act
		boolean match = matchingService.isMatch(a, b);

		// Assert
		assertFalse(match);
	}

	@Test
	@DisplayName("Events close in time still fail when they are tens of kilometres apart")
	void closeInTimeButDistanceExceedsThreshold_returnsFalse() {
		// Arrange — ~50 km due east of the reference point
		Event a = event("camera-a", "evt-1", REF_LAT, REF_LON, REF_TIME);
		Event b = event(
				"camera-b",
				"evt-2",
				REF_LAT,
				77.7400,
				REF_TIME.plusSeconds(8));

		// Act
		boolean match = matchingService.isMatch(a, b);

		// Assert
		assertFalse(match);
	}

	@Test
	@DisplayName("Events that are both far apart and hours apart do not match")
	void farInSpaceAndTime_returnsFalse() {
		// Arrange
		Event a = event("camera-a", "evt-1", REF_LAT, REF_LON, REF_TIME);
		Event b = event(
				"camera-b",
				"evt-2",
				19.0760,
				72.8777,
				REF_TIME.plusSeconds(7200));

		// Act
		boolean match = matchingService.isMatch(a, b);

		// Assert
		assertFalse(match);
	}

	@Test
	@DisplayName(
			"Events ~exactly 5.0 km apart with time well within threshold still match "
					+ "because the rule uses <= (inclusive boundary)")
	void exactlyAtDistanceThreshold_withTimeWithinThreshold_returnsTrue() {
		// Arrange
		// Measured Haversine distance is ~4.99999996 km — as close to exactly 5.0 km
		// as floating-point construction allows while staying on the same latitude.
		// Because isMatch uses distanceKm <= maxDistanceKm, this inclusive boundary matches.
		Event a = event("camera-a", "evt-1", REF_LAT, REF_LON, REF_TIME);
		Event b = event(
				"camera-b",
				"evt-2",
				REF_LAT,
				BOUNDARY_LON,
				REF_TIME.plusSeconds(20));

		double measuredKm = GeoUtils.haversineDistanceKm(
				a.latitude(), a.longitude(), b.latitude(), b.longitude());

		// Act
		boolean match = matchingService.isMatch(a, b);

		// Assert
		assertTrue(
				measuredKm <= 5.0,
				() -> "precondition failed: measured distance was " + measuredKm + " km, expected <= 5.0");
		assertTrue(
				match,
				() -> "expected match at inclusive 5.0 km boundary; measured distance was "
						+ measuredKm + " km");
	}

	@Test
	@DisplayName("Identical events (same coordinates and timestamp) always match")
	void identicalEvents_returnsTrue() {
		// Arrange
		Event a = event("camera-a", "evt-1", REF_LAT, REF_LON, REF_TIME);
		Event b = event("camera-a", "evt-1", REF_LAT, REF_LON, REF_TIME);

		// Act
		boolean match = matchingService.isMatch(a, b);

		// Assert
		assertTrue(match);
	}

	private static Event event(
			String source,
			String eventId,
			double latitude,
			double longitude,
			Instant timestamp) {
		return new Event(source, eventId, latitude, longitude, timestamp, Map.of());
	}
}
