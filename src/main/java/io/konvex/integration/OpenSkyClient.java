package io.konvex.integration;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
public class OpenSkyClient {

	private static final Logger log = LoggerFactory.getLogger(OpenSkyClient.class);

	private final RestClient restClient;

	public OpenSkyClient() {
		this.restClient = RestClient
				.builder()
				.baseUrl("https://opensky-network.org")
				.build();
	}

	public List<OpenSkyFlightState> fetchCurrentStates() {
		try {
			JsonNode response = restClient
					.get()
					.uri("/api/states/all")
					.retrieve()
					.body(JsonNode.class);

			if (response == null) {
				log.warn("OpenSky returned an empty response body");
				return List.of();
			}

			JsonNode statesNode = response.get("states");
			if (statesNode == null || !statesNode.isArray()) {
				log.warn("OpenSky response did not include a valid 'states' array");
				return List.of();
			}

			List<OpenSkyFlightState> flights = new ArrayList<>();
			for (JsonNode stateNode : statesNode) {
				if (!stateNode.isArray()) {
					continue;
				}

				String icao24 = textAt(stateNode, 0);
				String callsign = textAt(stateNode, 1);
				Double longitude = numberAt(stateNode, 5);
				Double latitude = numberAt(stateNode, 6);
				Double baroAltitude = numberAt(stateNode, 7);
				Long lastContact = longAt(stateNode, 4);

				flights.add(new OpenSkyFlightState(
						icao24,
						callsign,
						longitude,
						latitude,
						baroAltitude,
						lastContact));
			}

			return flights;
		} catch (RestClientException ex) {
			log.warn("Failed to fetch flight states from OpenSky: {}", ex.getMessage());
			return List.of();
		}
	}

	private static String textAt(JsonNode node, int index) {
		if (!node.has(index) || node.get(index).isNull()) {
			return null;
		}
		return node.get(index).asText();
	}

	private static Double numberAt(JsonNode node, int index) {
		if (!node.has(index) || node.get(index).isNull()) {
			return null;
		}
		return node.get(index).asDouble();
	}

	private static Long longAt(JsonNode node, int index) {
		if (!node.has(index) || node.get(index).isNull()) {
			return null;
		}
		return node.get(index).asLong();
	}

	public record OpenSkyFlightState(
			String icao24,
			String callsign,
			Double longitude,
			Double latitude,
			Double altitude,
			Long lastContactEpochSeconds) {
	}
}
