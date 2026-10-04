package io.konvex.persistence.repository;

import io.konvex.persistence.entity.EventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

	List<EventEntity> findBySourceAndEventIdOrderByEventTimestampDesc(String source, String eventId);
}