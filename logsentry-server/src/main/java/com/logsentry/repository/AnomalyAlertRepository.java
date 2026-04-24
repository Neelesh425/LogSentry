package com.logsentry.repository;

import com.logsentry.model.AnomalyAlert;
import com.logsentry.model.AnomalyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnomalyAlertRepository extends JpaRepository<AnomalyAlert, Long> {

    Page<AnomalyAlert> findAllByOrderByDetectedAtDesc(Pageable pageable);

    List<AnomalyAlert> findByResolvedFalseOrderByDetectedAtDesc();

    List<AnomalyAlert> findBySourceAndResolvedFalse(String source);

    @Query("SELECT COUNT(a) FROM AnomalyAlert a WHERE a.detectedAt >= :since")
    long countAlertsSince(@Param("since") Instant since);

    @Query("SELECT COUNT(a) FROM AnomalyAlert a WHERE a.resolved = false")
    long countActiveAlerts();

    List<AnomalyAlert> findByAnomalyTypeAndResolvedFalse(AnomalyType type);
}
