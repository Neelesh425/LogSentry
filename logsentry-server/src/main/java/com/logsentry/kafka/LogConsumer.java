package com.logsentry.kafka;

import com.logsentry.config.MetricsConfig;
import com.logsentry.detection.AnomalyDetectionEngine;
import com.logsentry.model.LogEvent;
import com.logsentry.repository.LogEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka consumer that ingests log events, persists them,
 * and feeds them into the anomaly detection engine.
 */
@Component
public class LogConsumer {

    private static final Logger log = LoggerFactory.getLogger(LogConsumer.class);

    private final LogEventRepository logEventRepository;
    private final AnomalyDetectionEngine detectionEngine;
    private final MetricsConfig metricsConfig;
    private final AtomicLong consumedCount = new AtomicLong(0);

    public LogConsumer(LogEventRepository logEventRepository,
                       AnomalyDetectionEngine detectionEngine,
                       MetricsConfig metricsConfig) {
        this.logEventRepository = logEventRepository;
        this.detectionEngine = detectionEngine;
        this.metricsConfig = metricsConfig;
    }

    @KafkaListener(
        topics = "${logsentry.kafka.topic:log-events}",
        groupId = "${spring.kafka.consumer.group-id:logsentry-consumer}",
        concurrency = "3"
    )
    public void consume(ConsumerRecord<String, LogEvent> record) {
        try {
            LogEvent event = record.value();

            // Persist to database
            LogEvent saved = logEventRepository.save(event);

            // Feed into anomaly detection
            detectionEngine.processEvent(saved);

            // Update metrics
            metricsConfig.incrementEventsIngested();
            consumedCount.incrementAndGet();

            // Update consumer lag estimate (simplified — true lag needs admin client)
            long lag = System.currentTimeMillis() - event.getTimestamp().toEpochMilli();
            metricsConfig.updateConsumerLag(lag);

        } catch (Exception e) {
            log.error("Error consuming log event from partition {} offset {}: {}",
                    record.partition(), record.offset(), e.getMessage(), e);
        }
    }

    public long getConsumedCount() {
        return consumedCount.get();
    }
}
