import { useState, useEffect, useRef } from 'react';
import { logsApi } from '../api';

const POLL_INTERVAL = 3000;

function formatTimestamp(ts) {
  if (!ts) return '—';
  const d = new Date(ts);
  return d.toLocaleTimeString('en-US', { hour12: false }) + '.' + String(d.getMilliseconds()).padStart(3, '0');
}

function LogStream() {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [error, setError] = useState(null);
  const intervalRef = useRef(null);

  const fetchLogs = async (pageNum) => {
    try {
      const res = await logsApi.getLogs(pageNum, 30);
      setLogs(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
      setError(null);
      setLoading(false);
    } catch (err) {
      setError('Unable to fetch logs');
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs(page);
    if (autoRefresh && page === 0) {
      intervalRef.current = setInterval(() => fetchLogs(0), POLL_INTERVAL);
    }
    return () => clearInterval(intervalRef.current);
  }, [page, autoRefresh]);

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
      </div>
    );
  }

  return (
    <div className="animate-in">
      <div className="page-header">
        <h2>Log Stream</h2>
        <p>Live log event feed from all sources</p>
      </div>

      <div className="data-table-wrapper">
        <div className="data-table-header">
          <h3>Recent Events</h3>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <button
              className={`btn ${autoRefresh ? 'btn-primary' : ''}`}
              onClick={() => setAutoRefresh(!autoRefresh)}
              id="toggle-auto-refresh"
            >
              {autoRefresh ? '⏸ Pause' : '▶ Live'}
            </button>
            <button className="btn" onClick={() => fetchLogs(page)} id="refresh-logs">
              ↻ Refresh
            </button>
          </div>
        </div>

        {error ? (
          <div className="empty-state">
            <div className="icon">⚠️</div>
            <p>{error}</p>
          </div>
        ) : logs.length === 0 ? (
          <div className="empty-state">
            <div className="icon">📋</div>
            <p>No log events yet. Start the backend to begin ingesting.</p>
          </div>
        ) : (
          <>
            <table className="data-table" id="log-stream-table">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Source</th>
                  <th>Level</th>
                  <th>Message</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log, idx) => (
                  <tr key={log.id || idx}>
                    <td className="timestamp-cell">{formatTimestamp(log.timestamp)}</td>
                    <td><span className="source-tag">{log.source}</span></td>
                    <td>
                      <span className={`badge ${(log.level || '').toLowerCase()}`}>
                        {log.level}
                      </span>
                    </td>
                    <td style={{ maxWidth: 400, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {log.message}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="pagination">
              <button className="btn" disabled={page === 0} onClick={() => setPage(p => p - 1)} id="prev-page">
                ← Prev
              </button>
              <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                Page {page + 1} of {totalPages || 1}
              </span>
              <button className="btn" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)} id="next-page">
                Next →
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default LogStream;
