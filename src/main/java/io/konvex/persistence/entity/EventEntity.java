package io.konvex.persistence.entity;

import io.konvex.model.Event;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false) private String source;
	@Column(name = "event_id", nullable = false) private String eventId;
	@Column(nullable = false) private double latitude;
	@Column(nullable = false) private double longitude;
	@Column(name = "event_timestamp", nullable = false) private Instant eventTimestamp;
	@Column(name = "metadata_json", nullable = false, columnDefinition = "TEXT") private String metadataJson;

	public static EventEntity from(Event event, String metadataJson) {
		return EventEntity.builder().source(event.source()).eventId(event.eventId()).latitude(event.latitude())
				.longitude(event.longitude()).eventTimestamp(event.timestamp()).metadataJson(metadataJson).build();
	}
}