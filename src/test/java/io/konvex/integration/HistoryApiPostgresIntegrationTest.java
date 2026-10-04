package io.konvex.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.konvex.model.Event;
import io.konvex.persistence.repository.CorrelationMatchRepository;
import io.konvex.persistence.repository.EventRepository;
import io.konvex.service.EventIngestionService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
		"konvex.opensky.enabled=false",
		"konvex.security.api-key=integration-key"
})
@AutoConfigureMockMvc
class HistoryApiPostgresIntegrationTest {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
			.withDatabaseName("konvex")
			.withUsername("konvex")
			.withPassword("konvex");

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EventIngestionService eventIngestionService;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private CorrelationMatchRepository matchRepository;

	@BeforeEach
	void cleanDatabase() {
		matchRepository.deleteAll();
		eventRepository.deleteAll();
	}

	@Test
	void persistsEventsAndMatchesAndExposesHistoryThroughApi() throws Exception {
		Instant baseTime = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
		Event first = event("camera-a", "evt-1", 28.6129, 77.2295, baseTime);
		Event second = event("camera-b", "evt-2", 28.6135, 77.2302, baseTime.plusSeconds(10));

		eventIngestionService.ingest(first);
		eventIngestionService.ingest(second);

		if (eventRepository.count() != 2) {
			throw new AssertionError("expected two persisted events");
		}
		if (matchRepository.count() != 1) {
			throw new AssertionError("expected one persisted correlation match");
		}

		mockMvc.perform(get("/api/matches")
					.param("from", baseTime.minusSeconds(1).toString())
					.param("to", baseTime.plusSeconds(60).toString())
					.param("source", "camera-b")
					.param("page", "0")
					.param("size", "10")
					.header("X-API-Key", "integration-key"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()", equalTo(1)))
				.andExpect(jsonPath("$.content[0].eventId", equalTo("evt-2")))
				.andExpect(jsonPath("$.content[0].matchedEventId", equalTo("evt-1")))
				.andExpect(jsonPath("$.content[0].matchedSource", equalTo("camera-a")));

		mockMvc.perform(get("/api/events/camera-a/evt-1")
					.header("X-API-Key", "integration-key"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$[0].source", equalTo("camera-a")))
				.andExpect(jsonPath("$[0].eventId", equalTo("evt-1")))
				.andExpect(jsonPath("$[0].metadata.type", equalTo("vehicle")));
	}

	@Test
	void historyApiRejectsMissingApiKey() throws Exception {
		mockMvc.perform(get("/api/matches"))
				.andExpect(status().isUnauthorized());
	}

	private static Event event(String source, String eventId, double latitude, double longitude, Instant timestamp) {
		Map<String, Object> metadata = "evt-1".equals(eventId) ? Map.of("type", "vehicle") : Map.of();
		return new Event(source, eventId, latitude, longitude, timestamp, metadata);
	}
}