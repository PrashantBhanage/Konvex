package io.konvex.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable domain model representing a single correlated event from an external source.
 *
 * <p>This record enforces the following validation rules in its compact canonical constructor:
 * <ul>
 *   <li>{@code source} must not be {@code null} or blank</li>
 *   <li>{@code eventId} must not be {@code null} or blank</li>
 *   <li>{@code latitude} must be in the closed range {@code [-90, 90]}</li>
 *   <li>{@code longitude} must be in the closed range {@code [-180, 180]}</li>
 *   <li>{@code timestamp} must not be {@code null}</li>
 *   <li>{@code metadata} must not be {@code null}; if {@code null} is passed, it defaults
 *       to an empty immutable map. Otherwise the map is wrapped with {@link Map#copyOf(Map)}
 *       so the event cannot be mutated via the caller's original map</li>
 * </ul>
 */
public record Event(
		String source,
		String eventId,
		double latitude,
		double longitude,
		Instant timestamp,
		Map<String, Object> metadata
) {

	public Event {
		if (source == null || source.isBlank()) {
			throw new IllegalArgumentException(
					"source must not be null or blank, but was: " + source);
		}
		if (eventId == null || eventId.isBlank()) {
			throw new IllegalArgumentException(
					"eventId must not be null or blank, but was: " + eventId);
		}
		if (latitude < -90 || latitude > 90) {
			throw new IllegalArgumentException(
					"latitude must be between -90 and 90 (inclusive), but was: " + latitude);
		}
		if (longitude < -180 || longitude > 180) {
			throw new IllegalArgumentException(
					"longitude must be between -180 and 180 (inclusive), but was: " + longitude);
		}
		if (timestamp == null) {
			throw new IllegalArgumentException("timestamp must not be null");
		}
		if (metadata == null) {
			metadata = Collections.emptyMap();
		} else {
			metadata = Map.copyOf(metadata);
		}
	}
}
