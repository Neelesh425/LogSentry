package com.logsentry.repository;

import com.logsentry.model.BaselineStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaselineStatsRepository extends JpaRepository<BaselineStats, Long> {

    List<BaselineStats> findBySourceOrderByWindowEndDesc(String source);

    @Query("SELECT b FROM BaselineStats b WHERE b.source = :source ORDER BY b.windowEnd DESC LIMIT :limit")
    List<BaselineStats> findRecentBySource(@Param("source") String source, @Param("limit") int limit);

    long countBySource(String source);

    void deleteBySource(String source);
}
