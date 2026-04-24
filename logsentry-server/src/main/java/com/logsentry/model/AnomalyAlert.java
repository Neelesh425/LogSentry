package com.logsentry.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "anomaly_alerts", indexes = {
    @Index(name = "idx_alert_type", columnList = "anomalyType"),
    @Index(name = "idx_alert_detected_at", columnList = "detectedAt"),
    @Index(name = "idx_alert_resolved", columnList = "resolved")
})
public class AnomalyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Column(nullable = false, length = 128)
    private String source;

    @Column(nullable = false)
    private Instant detectedAt;

    private Instant resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean resolved = false;

    private double currentValue;
    private double baselineMean;
    private double baselineStdDev;

    public AnomalyAlert() {}

    public AnomalyAlert(AnomalyType type, Severity severity, String source, String message,
                        double currentValue, double baselineMean, double baselineStdDev) {
        this.anomalyType = type;
        this.severity = severity;
        this.source = source;
        this.detectedAt = Instant.now();
        this.message = message;
        this.currentValue = currentValue;
        this.baselineMean = baselineMean;
        this.baselineStdDev = baselineStdDev;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AnomalyType getAnomalyType() { return anomalyType; }
    public void setAnomalyType(AnomalyType anomalyType) { this.anomalyType = anomalyType; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }

    public double getBaselineMean() { return baselineMean; }
    public void setBaselineMean(double baselineMean) { this.baselineMean = baselineMean; }

    public double getBaselineStdDev() { return baselineStdDev; }
    public void setBaselineStdDev(double baselineStdDev) { this.baselineStdDev = baselineStdDev; }
}
