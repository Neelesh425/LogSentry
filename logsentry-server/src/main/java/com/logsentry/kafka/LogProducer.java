package com.logsentry.kafka;

import com.logsentry.config.MetricsConfig;
import com.logsentry.model.LogEvent;
import com.logsentry.model.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Log event simulator — produces realistic log traffic to Kafka.
 * Generates ~80-100 events/sec with periodic anomaly bursts.
 */
@Component
public class LogProducer {

    private static final Logger log = LoggerFactory.getLogger(LogProducer.class);

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;
    private final MetricsConfig metricsConfig;
    private final Random random = new Random();
    private final AtomicLong sentCount = new AtomicLong(0);
    private volatile boolean enabled = true;

    private static final String[] SOURCES = {
        "auth-service", "api-gateway", "payment-service",
        "user-service", "order-service", "notification-service"
    };

    private static final String[] NORMAL_MESSAGES = {
        "Request processed successfully",
        "User authenticated",
        "Database query completed",
        "Cache hit for key",
        "Health check passed",
        "Session validated",
        "Configuration reloaded",
        "Scheduled task completed"
    };

    private static final String[] ERROR_MESSAGES = {
        "Connection timeout to database",
        "Authentication failed for user",
        "Rate limit exceeded",
        "Service unavailable",
        "OutOfMemoryError in worker thread",
        "Disk space critically low",
        "SSL certificate validation failed",
        "Deadlock detected in transaction"
    };

    @Value("${logsentry.kafka.topic:log-events}")
    private String topic;

    public LogProducer(KafkaTemplate<String, LogEvent> kafkaTemplate, MetricsConfig metricsConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.metricsConfig = metricsConfig;
    }

    /**
     * Sends a batch of log events every 100ms (~80-100 events/sec baseline).
     * Every 30 seconds, injects an anomaly burst of 10x-20x error volume.
     */
    @Scheduled(fixedRate = 100)
    public void produceLogEvents() {
        if (!enabled) return;

        try {
            long currentSecond = System.currentTimeMillis() / 1000;
            boolean anomalyBurst = (currentSecond % 60) >= 50; // burst in last 10s of each minute

            int batchSize = anomalyBurst ? random.nextInt(20) + 15 : random.nextInt(5) + 6;

            for (int i = 0; i < batchSize; i++) {
                LogEvent event = generateEvent(anomalyBurst);
                String sourceKey = event.getSource();
                kafkaTemplate.send(topic, sourceKey, event);
                sentCount.incrementAndGet();
            }
        } catch (Exception e) {
            log.warn("Failed to produce log events: {}", e.getMessage());
        }
    }

    /**
     * Periodically updates throughput metric based on actual send rate.
     */
    @Scheduled(fixedRate = 1000)
    public void updateThroughputMetric() {
        long count = sentCount.getAndSet(0);
        metricsConfig.updateThroughput(count);
    }

    private LogEvent generateEvent(boolean anomalyBurst) {
        String source = SOURCES[random.nextInt(SOURCES.length)];

        LogLevel level;
        String message;

        if (anomalyBurst && random.nextDouble() < 0.7) {
            // During anomaly: 70% errors
            level = random.nextBoolean() ? LogLevel.ERROR : LogLevel.FATAL;
            message = ERROR_MESSAGES[random.nextInt(ERROR_MESSAGES.length)];
        } else {
            // Normal distribution of log levels
            double r = random.nextDouble();
            if (r < 0.50) {
                level = LogLevel.INFO;
            } else if (r < 0.75) {
                level = LogLevel.DEBUG;
            } else if (r < 0.88) {
                level = LogLevel.WARN;
            } else if (r < 0.96) {
                level = LogLevel.ERROR;
            } else if (r < 0.99) {
                level = LogLevel.TRACE;
            } else {
                level = LogLevel.FATAL;
            }
            message = NORMAL_MESSAGES[random.nextInt(NORMAL_MESSAGES.length)];
        }

        LogEvent event = new LogEvent(source, level, message);
        event.setTimestamp(Instant.now());
        return event;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
