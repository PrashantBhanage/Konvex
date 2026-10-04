package io.konvex.persistence.repository;

import io.konvex.persistence.entity.CorrelationMatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CorrelationMatchRepository extends JpaRepository<CorrelationMatchEntity, Long>,
		JpaSpecificationExecutor<CorrelationMatchEntity> {

	Page<CorrelationMatchEntity> findAll(Specification<CorrelationMatchEntity> specification, Pageable pageable);
}