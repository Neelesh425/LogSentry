import { useState, useEffect, useRef } from 'react';
import { alertsApi } from '../api';

const POLL_INTERVAL = 3000;

function formatDate(ts) {
  if (!ts) return '—';
  const d = new Date(ts);
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ' ' +
    d.toLocaleTimeString('en-US', { hour12: false });
}

function Alerts() {
  const [alerts, setAlerts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState('all'); // all | active | resolved
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const intervalRef = useRef(null);

  const fetchAlerts = async () => {
    try {
      let res;
      if (filter === 'active') {
        res = await alertsApi.getActiveAlerts();
        setAlerts(res.data || []);
        setTotalPages(1);
      } else {
        res = await alertsApi.getAlerts(page, 30);
        const content = res.data.content || [];
        if (filter === 'resolved') {
          setAlerts(content.filter(a => a.resolved));
        } else {
          setAlerts(content);
        }
        setTotalPages(res.data.totalPages || 0);
      }
      setError(null);
      setLoading(false);
    } catch (err) {
      setError('Unable to fetch alerts');
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
    intervalRef.current = setInterval(fetchAlerts, POLL_INTERVAL);
    return () => clearInterval(intervalRef.current);
  }, [page, filter]);

  const handleResolve = async (id) => {
    try {
      await alertsApi.resolveAlert(id);
      fetchAlerts();
    } catch {
      // ignore
    }
  };

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
        <h2>Alerts</h2>
        <p>Anomaly alerts from the detection engine</p>
      </div>

      <div className="data-table-wrapper">
        <div className="data-table-header">
          <h3>Anomaly Alerts</h3>
          <div style={{ display: 'flex', gap: 8 }}>
            {['all', 'active', 'resolved'].map(f => (
              <button
                key={f}
                className={`btn ${filter === f ? 'btn-primary' : ''}`}
                onClick={() => { setFilter(f); setPage(0); }}
                id={`filter-${f}`}
              >
                {f.charAt(0).toUpperCase() + f.slice(1)}
              </button>
            ))}
          </div>
        </div>

        {error ? (
          <div className="empty-state">
            <div className="icon">⚠️</div>
            <p>{error}</p>
          </div>
        ) : alerts.length === 0 ? (
          <div className="empty-state">
            <div className="icon">✅</div>
            <p>No {filter !== 'all' ? filter : ''} alerts. System is healthy.</p>
          </div>
        ) : (
          <>
            <table className="data-table" id="alerts-table">
              <thead>
                <tr>
                  <th>Detected</th>
                  <th>Type</th>
                  <th>Severity</th>
                  <th>Source</th>
                  <th>Message</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map((alert, idx) => (
                  <tr key={alert.id || idx}>
                    <td className="timestamp-cell">{formatDate(alert.detectedAt)}</td>
                    <td>
                      <span className={`badge ${(alert.anomalyType || '').toLowerCase()}`}>
                        {(alert.anomalyType || '').replace('_', ' ')}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${(alert.severity || '').toLowerCase()}`}>
                        {alert.severity}
                      </span>
                    </td>
                    <td><span className="source-tag">{alert.source}</span></td>
                    <td style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {alert.message}
                    </td>
                    <td>
                      <span className={`badge ${alert.resolved ? 'resolved' : 'active'}`}>
                        {alert.resolved ? 'Resolved' : 'Active'}
                      </span>
                    </td>
                    <td>
                      {!alert.resolved && (
                        <button
                          className="btn btn-resolve"
                          onClick={() => handleResolve(alert.id)}
                          id={`resolve-${alert.id}`}
                        >
                          ✓ Resolve
                        </button>
                      )}
                      {alert.resolved && (
                        <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                          {formatDate(alert.resolvedAt)}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {filter !== 'active' && (
              <div className="pagination">
                <button className="btn" disabled={page === 0} onClick={() => setPage(p => p - 1)} id="alerts-prev">
                  ← Prev
                </button>
                <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                  Page {page + 1} of {totalPages || 1}
                </span>
                <button className="btn" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)} id="alerts-next">
                  Next →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default Alerts;
