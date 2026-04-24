package com.logsentry.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class MetricsConfig {

    private final AtomicLong throughputPerSec = new AtomicLong(0);
    private final AtomicLong consumerLag = new AtomicLong(0);
    private final AtomicLong activeAnomalies = new AtomicLong(0);

    private final Counter eventsIngested;
    private final Counter anomaliesDetected;

    public MetricsConfig(MeterRegistry registry) {
        eventsIngested = Counter.builder("logsentry_events_ingested_total")
                .description("Total number of log events ingested")
                .tag("application", "logsentry")
                .register(registry);

        anomaliesDetected = Counter.builder("logsentry_anomalies_detected_total")
                .description("Total number of anomalies detected")
                .tag("application", "logsentry")
                .register(registry);

        Gauge.builder("logsentry_throughput_per_sec", throughputPerSec, AtomicLong::get)
                .description("Current ingestion throughput per second")
                .tag("application", "logsentry")
                .register(registry);

        Gauge.builder("logsentry_consumer_lag", consumerLag, AtomicLong::get)
                .description("Kafka consumer lag")
                .tag("application", "logsentry")
                .register(registry);

        Gauge.builder("logsentry_active_anomalies", activeAnomalies, AtomicLong::get)
                .description("Currently active anomalies")
                .tag("application", "logsentry")
                .register(registry);
    }

    public void incrementEventsIngested() {
        eventsIngested.increment();
    }

    public void incrementAnomaliesDetected() {
        anomaliesDetected.increment();
    }

    public void updateThroughput(long value) {
        throughputPerSec.set(value);
    }

    public void updateConsumerLag(long value) {
        consumerLag.set(value);
    }

    public void updateActiveAnomalies(long value) {
        activeAnomalies.set(value);
    }

    public double getTotalEventsIngested() {
        return eventsIngested.count();
    }

    public double getTotalAnomaliesDetected() {
        return anomaliesDetected.count();
    }

    public long getThroughputPerSec() {
        return throughputPerSec.get();
    }

    public long getConsumerLag() {
        return consumerLag.get();
    }

    public long getActiveAnomalies() {
        return activeAnomalies.get();
    }
}
