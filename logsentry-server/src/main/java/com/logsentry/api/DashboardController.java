package com.logsentry.api;

import com.logsentry.config.MetricsConfig;
import com.logsentry.dto.DashboardStats;
import com.logsentry.repository.LogEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final MetricsConfig metricsConfig;
    private final LogEventRepository logEventRepository;

    public DashboardController(MetricsConfig metricsConfig, LogEventRepository logEventRepository) {
        this.metricsConfig = metricsConfig;
        this.logEventRepository = logEventRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        long eventsLastMinute = logEventRepository.countEventsSince(
                Instant.now().minusSeconds(60));

        DashboardStats stats = new DashboardStats(
                metricsConfig.getTotalEventsIngested(),
                metricsConfig.getTotalAnomaliesDetected(),
                metricsConfig.getThroughputPerSec(),
                metricsConfig.getConsumerLag(),
                metricsConfig.getActiveAnomalies(),
                eventsLastMinute
        );

        return ResponseEntity.ok(stats);
    }
}
