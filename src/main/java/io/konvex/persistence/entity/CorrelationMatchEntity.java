package io.konvex.persistence.entity;

import io.konvex.model.CorrelationMatch;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "correlation_matches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorrelationMatchEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "event_id", nullable = false) private String eventId;
	@Column(nullable = false) private String source;
	@Column(name = "matched_event_id", nullable = false) private String matchedEventId;
	@Column(name = "matched_source", nullable = false) private String matchedSource;
	@Column(name = "distance_km", nullable = false) private double distanceKm;
	@Column(name = "time_gap_seconds", nullable = false) private long timeGapSeconds;
	@Column(name = "detected_at", nullable = false) private Instant detectedAt;

	public static CorrelationMatchEntity from(CorrelationMatch match) {
		return CorrelationMatchEntity.builder().eventId(match.eventId()).source(match.source())
				.matchedEventId(match.matchedEventId()).matchedSource(match.matchedSource())
				.distanceKm(match.distanceKm()).timeGapSeconds(match.timeGapSeconds())
				.detectedAt(match.detectedAt()).build();
	}
}