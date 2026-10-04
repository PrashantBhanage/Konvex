package io.konvex.persistence.repository;
import io.konvex.persistence.entity.CorrelationMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CorrelationMatchRepository extends JpaRepository<CorrelationMatchEntity, Long> {
}