package com.logsentry.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "baseline_stats", indexes = {
    @Index(name = "idx_baseline_source", columnList = "source")
})
public class BaselineStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String source;

    @Column(nullable = false)
    private Instant windowStart;

    @Column(nullable = false)
    private Instant windowEnd;

    private double mean;
    private double stdDev;
    private long eventCount;

    public BaselineStats() {}

    public BaselineStats(String source, Instant windowStart, Instant windowEnd,
                         double mean, double stdDev, long eventCount) {
        this.source = source;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.mean = mean;
        this.stdDev = stdDev;
        this.eventCount = eventCount;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getWindowStart() { return windowStart; }
    public void setWindowStart(Instant windowStart) { this.windowStart = windowStart; }

    public Instant getWindowEnd() { return windowEnd; }
    public void setWindowEnd(Instant windowEnd) { this.windowEnd = windowEnd; }

    public double getMean() { return mean; }
    public void setMean(double mean) { this.mean = mean; }

    public double getStdDev() { return stdDev; }
    public void setStdDev(double stdDev) { this.stdDev = stdDev; }

    public long getEventCount() { return eventCount; }
    public void setEventCount(long eventCount) { this.eventCount = eventCount; }
}
