package io.konvex.service;

import io.konvex.controller.CorrelationMatchResponse;
import io.konvex.controller.EventHistoryResponse;
import io.konvex.controller.PageResponse;
import io.konvex.persistence.entity.CorrelationMatchEntity;
import io.konvex.persistence.repository.CorrelationMatchRepository;
import io.konvex.persistence.repository.EventRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class HistoryService {

	private static final int MAX_PAGE_SIZE = 100;

	private final CorrelationMatchRepository correlationMatchRepository;
	private final EventRepository eventRepository;
	private final ObjectMapper objectMapper;

	public PageResponse<CorrelationMatchResponse> findMatches(
			String fromValue, String toValue, String sourceValue, int page, int size) {
		validatePage(page, size);
		Instant from = parseInstant(fromValue, "from");
		Instant to = parseInstant(toValue, "to");
		String source = normalizeSource(sourceValue);

		if (from != null && to != null && from.isAfter(to)) {
			throw badRequest("from must be before or equal to to");
		}

		Specification<CorrelationMatchEntity> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
		if (from != null) {
			Instant value = from;
			specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("detectedAt"), value));
		}
		if (to != null) {
			Instant value = to;
			specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("detectedAt"), value));
		}
		if (source != null) {
			String value = source;
			specification = specification.and((root, query, cb) -> cb.equal(root.get("source"), value));
		}

		PageRequest pageable = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Direction.DESC, "detectedAt").and(Sort.by(Sort.Direction.DESC, "id")));
		Page<CorrelationMatchEntity> matches = correlationMatchRepository.findAll(specification, pageable);

		return new PageResponse<>(
				matches.getContent().stream().map(CorrelationMatchResponse::from).toList(),
				matches.getNumber(), matches.getSize(), matches.getTotalElements(), matches.getTotalPages());
	}

	public List<EventHistoryResponse> findEvents(String sourceValue, String eventIdValue) {
		String source = requirePathValue(sourceValue, "source");
		String eventId = requirePathValue(eventIdValue, "eventId");

		return eventRepository.findBySourceAndEventIdOrderByEventTimestampDesc(source, eventId)
				.stream()
				.map(entity -> EventHistoryResponse.from(entity, parseMetadata(entity.getMetadataJson())))
				.toList();
	}

	private Map<String, Object> parseMetadata(String metadataJson) {
		try {
			return objectMapper.readValue(metadataJson, Map.class);
		} catch (JacksonException | ClassCastException ex) {
			throw new IllegalStateException("Stored event metadata is invalid", ex);
		}
	}

	private static Instant parseInstant(String value, String parameter) {
		if (value == null || value.isBlank()) return null;
		try {
			return Instant.parse(value.trim());
		} catch (DateTimeParseException ex) {
			throw badRequest(parameter + " must be a valid ISO-8601 instant, for example 2026-03-15T10:30:00Z");
		}
	}

	private static String normalizeSource(String value) {
		if (value == null) return null;
		if (value.isBlank()) throw badRequest("source must not be blank");
		return value.trim();
	}

	private static String requirePathValue(String value, String parameter) {
		if (value == null || value.isBlank()) throw badRequest(parameter + " must not be blank");
		return value.trim();
	}

	private static void validatePage(int page, int size) {
		if (page < 0) throw badRequest("page must be greater than or equal to 0");
		if (size < 1 || size > MAX_PAGE_SIZE) throw badRequest("size must be between 1 and " + MAX_PAGE_SIZE);
	}

	private static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}
}