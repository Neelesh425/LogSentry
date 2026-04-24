package com.logsentry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "log_events", indexes = {
    @Index(name = "idx_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_log_source", columnList = "source"),
    @Index(name = "idx_log_level", columnList = "level")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 128)
    private String source;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private LogLevel level;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "event_count")
    private int eventCount = 1;

    public LogEvent() {}

    public LogEvent(String source, LogLevel level, String message) {
        this.timestamp = Instant.now();
        this.source = source;
        this.level = level;
        this.message = message;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LogLevel getLevel() { return level; }
    public void setLevel(LogLevel level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }

    @Override
    public String toString() {
        return "LogEvent{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", level=" + level +
                ", message='" + message + '\'' +
                '}';
    }
}
