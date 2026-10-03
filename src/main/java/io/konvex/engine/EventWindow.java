package io.konvex.engine;

import io.konvex.model.Event;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Component;

/**
 * Thread-safe sliding window of recently seen {@link Event}s for stream correlation.
 */
@Component
public class EventWindow {

	private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<>();

	/**
	 * Returns a live, allocation-free view of the events in the window.
	 */
	public Iterable<Event> getRecentEvents() {
		return events;
	}

	/**
	 * Adds a newly seen event and replaces any older observation with the same
	 * source and event ID.
	 */
	public void add(Event event) {
		events.removeIf(existing ->
				existing.source().equals(event.source())
						&& existing.eventId().equals(event.eventId()));
		events.add(event);
	}

	/**
	 * Removes observations that fall outside the configured event-time window.
	 * Using the incoming event timestamp keeps correlation correct when a source
	 * delivers events with a delayed or historical timestamp.
	 */
	public void evictExpired(Instant referenceTime, long maxTimeGapSeconds) {
		Instant cutoff = referenceTime.minusSeconds(maxTimeGapSeconds);
		events.removeIf(event -> event.timestamp().isBefore(cutoff));
	}
}
