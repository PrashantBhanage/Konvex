package io.konvex.engine;

import io.konvex.model.Event;
import java.time.Instant;
import java.util.List;

/**
 * Storage abstraction used by the correlation engine for recent event state.
 *
 * <p>The current implementation is in-memory and optimized for the hot matching
 * path. A separate persistent repository can provide history without changing
 * the correlation engine's contract.
 */
public interface EventStore {

	List<Event> getRecentEvents();

	List<Event> getNearbyEvents(Event reference, double maxDistanceKm);

	void add(Event event);

	void evictExpired(Instant referenceTime, long maxTimeGapSeconds);
}
