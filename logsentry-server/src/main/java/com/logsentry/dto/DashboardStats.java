package com.logsentry.dto;

public class DashboardStats {

    private double totalEventsIngested;
    private double totalAnomaliesDetected;
    private long throughputPerSec;
    private long consumerLag;
    private long activeAnomalies;
    private long eventsLastMinute;

    public DashboardStats() {}

    public DashboardStats(double totalEventsIngested, double totalAnomaliesDetected,
                          long throughputPerSec, long consumerLag, long activeAnomalies,
                          long eventsLastMinute) {
        this.totalEventsIngested = totalEventsIngested;
        this.totalAnomaliesDetected = totalAnomaliesDetected;
        this.throughputPerSec = throughputPerSec;
        this.consumerLag = consumerLag;
        this.activeAnomalies = activeAnomalies;
        this.eventsLastMinute = eventsLastMinute;
    }

    public double getTotalEventsIngested() { return totalEventsIngested; }
    public void setTotalEventsIngested(double v) { this.totalEventsIngested = v; }

    public double getTotalAnomaliesDetected() { return totalAnomaliesDetected; }
    public void setTotalAnomaliesDetected(double v) { this.totalAnomaliesDetected = v; }

    public long getThroughputPerSec() { return throughputPerSec; }
    public void setThroughputPerSec(long v) { this.throughputPerSec = v; }

    public long getConsumerLag() { return consumerLag; }
    public void setConsumerLag(long v) { this.consumerLag = v; }

    public long getActiveAnomalies() { return activeAnomalies; }
    public void setActiveAnomalies(long v) { this.activeAnomalies = v; }

    public long getEventsLastMinute() { return eventsLastMinute; }
    public void setEventsLastMinute(long v) { this.eventsLastMinute = v; }
}
