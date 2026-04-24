import { useState, useEffect, useRef } from 'react';
import { dashboardApi, alertsApi, logsApi } from '../api';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, BarChart, Bar, Legend
} from 'recharts';

const POLL_INTERVAL = 2000;

function formatNumber(num) {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
  return Math.round(num).toString();
}

function Dashboard() {
  const [stats, setStats] = useState(null);
  const [throughputHistory, setThroughputHistory] = useState([]);
  const [anomalyHistory, setAnomalyHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const intervalRef = useRef(null);

  const fetchStats = async () => {
    try {
      const res = await dashboardApi.getStats();
      const data = res.data;
      setStats(data);

      const now = new Date().toLocaleTimeString('en-US', { hour12: false });

      setThroughputHistory(prev => {
        const updated = [...prev, { time: now, throughput: data.throughputPerSec, events: data.eventsLastMinute }];
        return updated.slice(-30);
      });

      setAnomalyHistory(prev => {
        const updated = [...prev, { time: now, anomalies: data.activeAnomalies, detected: data.totalAnomaliesDetected }];
        return updated.slice(-30);
      });

      setError(null);
      setLoading(false);
    } catch (err) {
      setError('Unable to connect to LogSentry server. Make sure the backend is running on port 8080.');
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
    intervalRef.current = setInterval(fetchStats, POLL_INTERVAL);
    return () => clearInterval(intervalRef.current);
  }, []);

  const customTooltipStyle = {
    backgroundColor: 'rgba(17, 24, 39, 0.95)',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    borderRadius: '8px',
    padding: '10px 14px',
    fontSize: '12px',
    color: '#f1f5f9',
    boxShadow: '0 4px 20px rgba(0,0,0,0.4)',
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <div className="page-header">
          <h2>Dashboard</h2>
          <p>Real-time log anomaly detection overview</p>
        </div>
        <div className="empty-state">
          <div className="icon">⚠️</div>
          <p>{error}</p>
          <p style={{ marginTop: 8, fontSize: 12, color: 'var(--text-muted)' }}>
            Run the Spring Boot server: <code>cd logsentry-server && mvn spring-boot:run</code>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="animate-in">
      <div className="page-header">
        <h2>Dashboard</h2>
        <p>Real-time log anomaly detection overview</p>
      </div>

      {/* Stats Grid */}
      <div className="stats-grid">
        <div className="stat-card indigo">
          <div className="stat-label">Events Ingested</div>
          <div className="stat-value">
            {formatNumber(stats?.totalEventsIngested || 0)}
          </div>
        </div>
        <div className="stat-card cyan">
          <div className="stat-label">Throughput</div>
          <div className="stat-value">
            {stats?.throughputPerSec || 0}
            <span className="stat-unit">/sec</span>
          </div>
        </div>
        <div className="stat-card amber">
          <div className="stat-label">Events / Min</div>
          <div className="stat-value">
            {formatNumber(stats?.eventsLastMinute || 0)}
          </div>
        </div>
        <div className="stat-card rose">
          <div className="stat-label">Active Anomalies</div>
          <div className="stat-value">
            {stats?.activeAnomalies || 0}
          </div>
        </div>
        <div className="stat-card emerald">
          <div className="stat-label">Consumer Lag</div>
          <div className="stat-value">
            {stats?.consumerLag || 0}
            <span className="stat-unit">ms</span>
          </div>
        </div>
      </div>

      {/* Charts */}
      <div className="charts-grid">
        {/* Throughput Chart */}
        <div className="chart-card">
          <h3>
            <span className="chart-dot" style={{ background: 'var(--accent-cyan)' }}></span>
            Ingestion Throughput
          </h3>
          <ResponsiveContainer width="100%" height={260}>
            <AreaChart data={throughputHistory}>
              <defs>
                <linearGradient id="gradThroughput" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#06b6d4" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#06b6d4" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis dataKey="time" stroke="#64748b" fontSize={10} tickLine={false} />
              <YAxis stroke="#64748b" fontSize={10} tickLine={false} />
              <Tooltip contentStyle={customTooltipStyle} />
              <Area type="monotone" dataKey="throughput" stroke="#06b6d4" strokeWidth={2}
                    fill="url(#gradThroughput)" name="Events/sec" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Anomaly Chart */}
        <div className="chart-card">
          <h3>
            <span className="chart-dot" style={{ background: 'var(--accent-rose)' }}></span>
            Anomaly Detection
          </h3>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={anomalyHistory}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis dataKey="time" stroke="#64748b" fontSize={10} tickLine={false} />
              <YAxis stroke="#64748b" fontSize={10} tickLine={false} />
              <Tooltip contentStyle={customTooltipStyle} />
              <Bar dataKey="anomalies" fill="#f43f5e" name="Active Anomalies" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
