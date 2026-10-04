package io.konvex.integration;

import io.konvex.integration.OpenSkyClient.OpenSkyFlightState;
import io.konvex.model.Event;
import io.konvex.service.EventIngestionService;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "konvex.opensky.enabled", havingValue = "true", matchIfMissing = true)
public class OpenSkyPoller {
	private static final Logger log = LoggerFactory.getLogger(OpenSkyPoller.class);
	private final OpenSkyClient openSkyClient;
	private final EventIngestionService eventIngestionService;

	public OpenSkyPoller(OpenSkyClient openSkyClient, EventIngestionService eventIngestionService) {
		this.openSkyClient = openSkyClient;
		this.eventIngestionService = eventIngestionService;
	}

	@Scheduled(fixedRateString = "${konvex.opensky.poll-interval-ms:30000}", initialDelayString = "${konvex.opensky.initial-delay-ms:5000}")
	public void pollAndProcessFlights() {
		long startedNanos = System.nanoTime();
		List<OpenSkyFlightState> flights = openSkyClient.fetchCurrentStates();
		if (flights.isEmpty()) { log.warn("OpenSky poll returned no flight states"); return; }
		int processed = 0; int skipped = 0;
		for (OpenSkyFlightState flight : flights) {
			if (flight.latitude() == null || flight.longitude() == null) { skipped++; continue; }
			String eventId = normalizeEventId(flight.icao24());
			if (eventId == null) { skipped++; continue; }
			Instant timestamp = flight.lastContactEpochSeconds() == null ? Instant.now() : Instant.ofEpochSecond(flight.lastContactEpochSeconds());
			try {
				eventIngestionService.ingest(new Event("OpenSky", eventId, flight.latitude(), flight.longitude(), timestamp, buildMetadata(flight)));
				processed++;
			} catch (IllegalArgumentException ex) {
				skipped++; log.warn("Skipping invalid OpenSky flight eventId={}: {}", eventId, ex.getMessage());
			}
		}
		long durationMs = (System.nanoTime() - startedNanos) / 1_000_000;
		log.info("OpenSky poll complete: total={}, processed={}, skipped={}, durationMs={}", flights.size(), processed, skipped, durationMs);
	}

	private static String normalizeEventId(String icao24) {
		if (icao24 == null) return null;
		String trimmed = icao24.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
	private static String normalizeCallsign(String callsign) {
		if (callsign == null) return null;
		String trimmed = callsign.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
	private static Map<String, Object> buildMetadata(OpenSkyFlightState flight) {
		Map<String, Object> metadata = new HashMap<>();
		String callsign = normalizeCallsign(flight.callsign());
		if (callsign != null) metadata.put("callsign", callsign);
		if (flight.altitude() != null) metadata.put("altitude", flight.altitude());
		return metadata;
	}
}