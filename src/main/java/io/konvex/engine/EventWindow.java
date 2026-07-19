package io.konvex.engine;

import io.konvex.model.Event;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Component;

/**
 * Thread-safe sliding window of recently seen {@link Event}s for stream correlation.
 */
@Component
public class EventWindow {

	private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<>();

	/**
	 * Returns a snapshot of events currently held in the window.
	 */
	public List<Event> getRecentEvents() {
		return List.copyOf(events);
	}

	/**
	 * Adds a newly seen event to the window.
	 */
	public void add(Event event) {
		events.add(event);
	}

	/**
	 * Removes events whose timestamps are older than {@code maxTimeGapSeconds}
	 * relative to {@code now}.
	 *
	 * @param now               reference instant (typically the incoming event's timestamp)
	 * @param maxTimeGapSeconds maximum age in seconds to retain
	 */
	public void evictExpired(Instant now, long maxTimeGapSeconds) {
		Instant cutoff = now.minusSeconds(maxTimeGapSeconds);
		events.removeIf(event -> event.timestamp().isBefore(cutoff));
	}
}
