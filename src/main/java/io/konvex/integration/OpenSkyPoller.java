package io.konvex.integration;

import io.konvex.engine.CorrelationEngine;
import io.konvex.integration.OpenSkyClient.OpenSkyFlightState;
import io.konvex.model.Event;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OpenSkyPoller {

	private static final Logger log = LoggerFactory.getLogger(OpenSkyPoller.class);

	private final OpenSkyClient openSkyClient;
	private final CorrelationEngine correlationEngine;

	public OpenSkyPoller(OpenSkyClient openSkyClient, CorrelationEngine correlationEngine) {
		this.openSkyClient = openSkyClient;
		this.correlationEngine = correlationEngine;
	}

	@Scheduled(fixedRate = 30_000L, initialDelay = 5_000L)
	public void pollAndProcessFlights() {
		List<OpenSkyFlightState> flights = openSkyClient.fetchCurrentStates();
		if (flights.isEmpty()) {
			log.warn("OpenSky poll returned no flight states");
			return;
		}

		int processed = 0;
		int skipped = 0;

		for (OpenSkyFlightState flight : flights) {
			if (flight.latitude() == null || flight.longitude() == null) {
				skipped++;
				continue;
			}

			String eventId = normalizeEventId(flight.icao24());
			if (eventId == null) {
				skipped++;
				continue;
			}

			Instant timestamp = flight.lastContactEpochSeconds() == null
					? Instant.now()
					: Instant.ofEpochSecond(flight.lastContactEpochSeconds());

			Map<String, Object> metadata = buildMetadata(flight);

			try {
				Event event = new Event(
						"OpenSky",
						eventId,
						flight.latitude(),
						flight.longitude(),
						timestamp,
						metadata);

				correlationEngine.processEvent(event);
				processed++;
			} catch (IllegalArgumentException ex) {
				skipped++;
				log.warn("Skipping invalid OpenSky flight eventId={}: {}", eventId, ex.getMessage());
			}
		}

		log.info("OpenSky poll complete: total={}, processed={}, skipped={}",
				flights.size(), processed, skipped);
	}

	private static String normalizeEventId(String icao24) {
		if (icao24 == null) {
			return null;
		}
		String trimmed = icao24.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String normalizeCallsign(String callsign) {
		if (callsign == null) {
			return null;
		}
		String trimmed = callsign.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static Map<String, Object> buildMetadata(OpenSkyFlightState flight) {
		Map<String, Object> metadata = new HashMap<>();
		String callsign = normalizeCallsign(flight.callsign());
		if (callsign != null) {
			metadata.put("callsign", callsign);
		}
		if (flight.altitude() != null) {
			metadata.put("altitude", flight.altitude());
		}
		return metadata;
	}
}
