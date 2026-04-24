package com.logsentry.detection;

import com.logsentry.config.MetricsConfig;
import com.logsentry.model.*;
import com.logsentry.repository.AnomalyAlertRepository;
import com.logsentry.repository.BaselineStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core anomaly detection engine using sliding-window statistics.
 * <p>
 * Detection strategies:
 * <ul>
 *   <li>SPIKE: current count > mean + spikeThresholdSigma * stdDev</li>
 *   <li>GRADUAL_DRIFT: rolling mean shifts > driftThresholdSigma * historical stdDev</li>
 *   <li>COLD_START: suppresses alerts during initial learning windows</li>
 *   <li>NOISY_BASELINE: requires sustained anomaly for N consecutive windows</li>
 * </ul>
 */
@Service
public class AnomalyDetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionEngine.class);

    private final AnomalyAlertRepository alertRepository;
    private final BaselineStatsRepository baselineStatsRepository;
    private final MetricsConfig metricsConfig;

    // Per-source sliding window state
    private final ConcurrentHashMap<String, SourceState> sourceStates = new ConcurrentHashMap<>();

    @Value("${logsentry.detection.window-size-seconds:10}")
    private int windowSizeSeconds;

    @Value("${logsentry.detection.learning-windows:6}")
    private int learningWindows;

    @Value("${logsentry.detection.spike-threshold-sigma:3.0}")
    private double spikeThresholdSigma;

    @Value("${logsentry.detection.drift-threshold-sigma:2.0}")
    private double driftThresholdSigma;

    @Value("${logsentry.detection.sustained-windows:3}")
    private int sustainedWindows;

    public AnomalyDetectionEngine(AnomalyAlertRepository alertRepository,
                                   BaselineStatsRepository baselineStatsRepository,
                                   MetricsConfig metricsConfig) {
        this.alertRepository = alertRepository;
        this.baselineStatsRepository = baselineStatsRepository;
        this.metricsConfig = metricsConfig;
    }

    /**
     * Process a single log event — updates the sliding window for its source.
     */
    public void processEvent(LogEvent event) {
        String source = event.getSource();
        SourceState state = sourceStates.computeIfAbsent(source, k -> new SourceState());
        state.currentWindowCount.incrementAndGet();
    }

    /**
     * Runs every window interval — evaluates each source for anomalies.
     */
    @Scheduled(fixedRateString = "${logsentry.detection.window-size-seconds:10}000")
    public void evaluateWindows() {
        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minusSeconds(windowSizeSeconds);

        for (Map.Entry<String, SourceState> entry : sourceStates.entrySet()) {
            String source = entry.getKey();
            SourceState state = entry.getValue();

            long currentCount = state.currentWindowCount.getAndSet(0);
            state.windowHistory.addLast(currentCount);

            // Keep a bounded history
            if (state.windowHistory.size() > 100) {
                state.windowHistory.removeFirst();
            }

            state.totalWindows++;

            // Cold-start: still learning
            if (state.totalWindows <= learningWindows) {
                log.debug("Source {} in cold-start phase (window {}/{})", source, state.totalWindows, learningWindows);
                saveBaseline(source, windowStart, windowEnd, currentCount, state);
                continue;
            }

            // Compute baseline stats from history (excluding current window)
            double[] stats = computeBaselineStats(state);
            double mean = stats[0];
            double stdDev = stats[1];

            // Save baseline
            saveBaseline(source, windowStart, windowEnd, mean, stdDev, currentCount);

            // --- Spike Detection ---
            if (stdDev > 0 && currentCount > mean + spikeThresholdSigma * stdDev) {
                state.consecutiveAnomalyWindows++;

                // Check if this is noisy baseline (need sustained anomaly)
                if (isNoisyBaseline(state)) {
                    if (state.consecutiveAnomalyWindows >= sustainedWindows) {
                        Severity severity = currentCount > mean + 5 * stdDev ? Severity.CRITICAL : Severity.HIGH;
                        raiseAlert(AnomalyType.SPIKE, severity, source,
                                String.format("Sustained spike: %d events (baseline: %.1f ± %.1f, %d consecutive anomalous windows)",
                                        currentCount, mean, stdDev, state.consecutiveAnomalyWindows),
                                currentCount, mean, stdDev);
                    }
                } else {
                    Severity severity = determineSeverity(currentCount, mean, stdDev);
                    raiseAlert(AnomalyType.SPIKE, severity, source,
                            String.format("Spike detected: %d events (baseline: %.1f ± %.1f)",
                                    currentCount, mean, stdDev),
                            currentCount, mean, stdDev);
                }
            } else {
                // Reset consecutive counter on normal window
                if (state.consecutiveAnomalyWindows > 0) {
                    state.consecutiveAnomalyWindows = 0;
                    resolveAlerts(source);
                }
            }

            // --- Gradual Drift Detection ---
            if (state.windowHistory.size() >= learningWindows * 2) {
                double recentMean = computeRecentMean(state, learningWindows);
                double historicalMean = computeHistoricalMean(state, learningWindows);
                double historicalStdDev = computeHistoricalStdDev(state, learningWindows, historicalMean);

                if (historicalStdDev > 0) {
                    double drift = Math.abs(recentMean - historicalMean);
                    if (drift > driftThresholdSigma * historicalStdDev) {
                        raiseAlert(AnomalyType.GRADUAL_DRIFT, Severity.MEDIUM, source,
                                String.format("Gradual drift: recent mean %.1f vs historical %.1f (drift: %.1f, threshold: %.1f)",
                                        recentMean, historicalMean, drift, driftThresholdSigma * historicalStdDev),
                                recentMean, historicalMean, historicalStdDev);
                    }
                }
            }
        }

        // Update active anomaly count
        metricsConfig.updateActiveAnomalies(alertRepository.countActiveAlerts());
    }

    /**
     * Manually evaluate a list of event counts (for testing).
     */
    public List<AnomalyAlert> evaluateManual(String source, List<Long> windowCounts) {
        List<AnomalyAlert> alerts = new ArrayList<>();
        SourceState state = new SourceState();

        for (int i = 0; i < windowCounts.size(); i++) {
            long count = windowCounts.get(i);
            state.windowHistory.addLast(count);
            state.totalWindows++;

            if (state.totalWindows <= learningWindows) {
                continue; // cold-start
            }

            double[] stats = computeBaselineStats(state);
            double mean = stats[0];
            double stdDev = stats[1];

            // Spike check
            if (stdDev > 0 && count > mean + spikeThresholdSigma * stdDev) {
                state.consecutiveAnomalyWindows++;
                if (!isNoisyBaseline(state) || state.consecutiveAnomalyWindows >= sustainedWindows) {
                    alerts.add(new AnomalyAlert(AnomalyType.SPIKE, determineSeverity(count, mean, stdDev),
                            source, String.format("Spike: %d events (mean=%.1f, stddev=%.1f)", count, mean, stdDev),
                            count, mean, stdDev));
                }
            } else {
                state.consecutiveAnomalyWindows = 0;
            }

            // Drift check
            if (state.windowHistory.size() >= learningWindows * 2) {
                double recentMean = computeRecentMean(state, learningWindows);
                double historicalMean = computeHistoricalMean(state, learningWindows);
                double historicalStdDev = computeHistoricalStdDev(state, learningWindows, historicalMean);

                if (historicalStdDev > 0) {
                    double drift = Math.abs(recentMean - historicalMean);
                    if (drift > driftThresholdSigma * historicalStdDev) {
                        alerts.add(new AnomalyAlert(AnomalyType.GRADUAL_DRIFT, Severity.MEDIUM, source,
                                String.format("Drift: recent=%.1f historical=%.1f", recentMean, historicalMean),
                                recentMean, historicalMean, historicalStdDev));
                    }
                }
            }
        }
        return alerts;
    }

    // --- Private helpers ---

    double[] computeBaselineStats(SourceState state) {
        List<Long> history = new ArrayList<>(state.windowHistory);
        // Exclude the last entry (current window) from baseline
        if (history.size() > 1) {
            history = history.subList(0, history.size() - 1);
        }

        double mean = history.stream().mapToLong(l -> l).average().orElse(0.0);
        double variance = history.stream().mapToDouble(l -> Math.pow(l - mean, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        return new double[]{mean, stdDev};
    }

    private boolean isNoisyBaseline(SourceState state) {
        // Consider baseline noisy if stdDev relative to mean is high (CV > 0.5)
        double[] stats = computeBaselineStats(state);
        double mean = stats[0];
        double stdDev = stats[1];
        if (mean <= 0) return false;
        return (stdDev / mean) > 0.5;
    }

    private Severity determineSeverity(double currentCount, double mean, double stdDev) {
        if (stdDev <= 0) return Severity.LOW;
        double zScore = (currentCount - mean) / stdDev;
        if (zScore > 6) return Severity.CRITICAL;
        if (zScore > 4.5) return Severity.HIGH;
        if (zScore > 3.5) return Severity.MEDIUM;
        return Severity.LOW;
    }

    private double computeRecentMean(SourceState state, int windowCount) {
        List<Long> history = new ArrayList<>(state.windowHistory);
        int start = Math.max(0, history.size() - windowCount);
        return history.subList(start, history.size()).stream().mapToLong(l -> l).average().orElse(0.0);
    }

    private double computeHistoricalMean(SourceState state, int windowCount) {
        List<Long> history = new ArrayList<>(state.windowHistory);
        int end = Math.max(0, history.size() - windowCount);
        int start = Math.max(0, end - windowCount);
        return history.subList(start, end).stream().mapToLong(l -> l).average().orElse(0.0);
    }

    private double computeHistoricalStdDev(SourceState state, int windowCount, double mean) {
        List<Long> history = new ArrayList<>(state.windowHistory);
        int end = Math.max(0, history.size() - windowCount);
        int start = Math.max(0, end - windowCount);
        double variance = history.subList(start, end).stream()
                .mapToDouble(l -> Math.pow(l - mean, 2))
                .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    private void raiseAlert(AnomalyType type, Severity severity, String source,
                            String message, double currentValue, double mean, double stdDev) {
        // Avoid duplicate active alerts for same source/type
        List<AnomalyAlert> existing = alertRepository.findBySourceAndResolvedFalse(source);
        boolean duplicate = existing.stream()
                .anyMatch(a -> a.getAnomalyType() == type);
        if (duplicate) return;

        AnomalyAlert alert = new AnomalyAlert(type, severity, source, message, currentValue, mean, stdDev);
        alertRepository.save(alert);
        metricsConfig.incrementAnomaliesDetected();
        log.warn("🚨 ANOMALY DETECTED [{}] {}: {} (current={}, mean={}, stdDev={})",
                type, source, message, currentValue, mean, stdDev);
    }

    private void resolveAlerts(String source) {
        List<AnomalyAlert> activeAlerts = alertRepository.findBySourceAndResolvedFalse(source);
        for (AnomalyAlert alert : activeAlerts) {
            if (alert.getAnomalyType() == AnomalyType.SPIKE) {
                alert.setResolved(true);
                alert.setResolvedAt(Instant.now());
                alertRepository.save(alert);
                log.info("✅ Alert resolved for source {}: {}", source, alert.getMessage());
            }
        }
    }

    private void saveBaseline(String source, Instant start, Instant end, long count, SourceState state) {
        double mean = state.windowHistory.stream().mapToLong(l -> l).average().orElse(0);
        double variance = state.windowHistory.stream().mapToDouble(l -> Math.pow(l - mean, 2)).average().orElse(0);
        BaselineStats baseline = new BaselineStats(source, start, end, mean, Math.sqrt(variance), count);
        baselineStatsRepository.save(baseline);
    }

    private void saveBaseline(String source, Instant start, Instant end, double mean, double stdDev, long count) {
        BaselineStats baseline = new BaselineStats(source, start, end, mean, stdDev, count);
        baselineStatsRepository.save(baseline);
    }

    // For testing: access source states
    public ConcurrentHashMap<String, SourceState> getSourceStates() {
        return sourceStates;
    }

    // For testing: override config values
    public void setWindowSizeSeconds(int v) { this.windowSizeSeconds = v; }
    public void setLearningWindows(int v) { this.learningWindows = v; }
    public void setSpikeThresholdSigma(double v) { this.spikeThresholdSigma = v; }
    public void setDriftThresholdSigma(double v) { this.driftThresholdSigma = v; }
    public void setSustainedWindows(int v) { this.sustainedWindows = v; }

    /**
     * Per-source sliding window state.
     */
    public static class SourceState {
        public final LinkedList<Long> windowHistory = new LinkedList<>();
        public final AtomicLong currentWindowCount = new AtomicLong(0);
        public int totalWindows = 0;
        public int consecutiveAnomalyWindows = 0;
    }
}
