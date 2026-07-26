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
	 * <p>
	 * {@link ConcurrentLinkedQueue} iteration is weakly-consistent and safe
	 * without allocating a separate copy. Callers must not modify the queue
	 * through this reference.
	 */
	public Iterable<Event> getRecentEvents() {
		return events;
	}

	/**
	 * Adds a newly seen event to the window.
	 */
	public void add(Event event) {
		events.add(event);
	}

	/**
	 * Removes events that are older than {@code maxTimeGapSeconds} relative to
	 * wall-clock time now.
	 * <p>
	 * Wall-clock ({@link Instant#now()}) is used deliberately: OpenSky
	 * {@code lastContact} timestamps can lag several minutes behind real time,
	 * so using the incoming event's timestamp would cause events from previous
	 * poll cycles to appear "fresh" and accumulate unboundedly.
	 *
	 * @param maxTimeGapSeconds maximum age in seconds to retain
	 */
	public void evictExpired(long maxTimeGapSeconds) {
		Instant cutoff = Instant.now().minusSeconds(maxTimeGapSeconds);
		events.removeIf(event -> event.timestamp().isBefore(cutoff));
	}
}
