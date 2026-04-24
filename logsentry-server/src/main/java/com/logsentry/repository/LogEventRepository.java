package com.logsentry.repository;

import com.logsentry.model.LogEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogEventRepository extends JpaRepository<LogEvent, Long> {

    Page<LogEvent> findAllByOrderByTimestampDesc(Pageable pageable);

    List<LogEvent> findBySourceAndTimestampBetween(String source, Instant start, Instant end);

    @Query("SELECT COUNT(e) FROM LogEvent e WHERE e.timestamp >= :since")
    long countEventsSince(@Param("since") Instant since);

    @Query("SELECT DISTINCT e.source FROM LogEvent e")
    List<String> findDistinctSources();

    @Query("SELECT COUNT(e) FROM LogEvent e WHERE e.source = :source AND e.timestamp BETWEEN :start AND :end")
    long countBySourceAndWindow(@Param("source") String source,
                                @Param("start") Instant start,
                                @Param("end") Instant end);
}
