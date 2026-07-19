package io.konvex.engine;

import io.konvex.model.Event;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Feeds a short hardcoded event sequence into {@link CorrelationEngine} at
 * startup so correlation output can be observed live in the console.
 */
@Component
public class CorrelationDemoRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(CorrelationDemoRunner.class);

	private static final double INDIA_GATE_LAT = 28.6129;
	private static final double INDIA_GATE_LON = 77.2295;
	private static final Instant T0 = Instant.parse("2026-03-15T10:30:00Z");

	private final CorrelationEngine correlationEngine;

	public CorrelationDemoRunner(CorrelationEngine correlationEngine) {
		this.correlationEngine = correlationEngine;
	}

	@Override
	public void run(String... args) throws InterruptedException {
		List<Event> sequence = List.of(
				// 1 — empty window → unmatched
				event("camera-north", "n-001", INDIA_GATE_LAT, INDIA_GATE_LON, T0),
				// 2 — ~100 m away, 15 s later → MATCHED with n-001
				event("camera-south", "s-001", 28.6135, 77.2302, T0.plusSeconds(15)),
				// 3 — ~50 km east, still within time window → no match
				event("camera-east", "e-001", INDIA_GATE_LAT, 77.7400, T0.plusSeconds(25)),
				// 4 — near India Gate but 200 s later → prior events evicted → unmatched
				event("camera-west", "w-001", 28.6130, 77.2298, T0.plusSeconds(200)),
				// 5 — near w-001, 10 s later → MATCHED with w-001
				event("camera-north", "n-002", 28.6132, 77.2300, T0.plusSeconds(210)),
				// 6 — Mumbai, close in event-time to n-002 → no match
				event("camera-mobile", "m-001", 19.0760, 72.8777, T0.plusSeconds(220)));

		log.info("=== Correlation demo starting ({} events) ===", sequence.size());
		for (Event event : sequence) {
			correlationEngine.processEvent(event);
			Thread.sleep(700);
		}
		log.info("=== Correlation demo finished ===");
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
