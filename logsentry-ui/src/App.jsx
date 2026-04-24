import { useState, useEffect, useCallback } from 'react';
import Dashboard from './components/Dashboard';
import LogStream from './components/LogStream';
import Alerts from './components/Alerts';
import './App.css';

const PAGES = {
  DASHBOARD: 'dashboard',
  LOGS: 'logs',
  ALERTS: 'alerts',
};

function App() {
  const [activePage, setActivePage] = useState(PAGES.DASHBOARD);

  const renderPage = useCallback(() => {
    switch (activePage) {
      case PAGES.LOGS:
        return <LogStream />;
      case PAGES.ALERTS:
        return <Alerts />;
      default:
        return <Dashboard />;
    }
  }, [activePage]);

  return (
    <div className="app-layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-logo">
          <div className="logo-icon">🛡️</div>
          <h1>LogSentry</h1>
        </div>

        <nav className="sidebar-nav">
          <button
            className={`nav-link ${activePage === PAGES.DASHBOARD ? 'active' : ''}`}
            onClick={() => setActivePage(PAGES.DASHBOARD)}
          >
            <span className="icon">📊</span>
            <span>Dashboard</span>
          </button>
          <button
            className={`nav-link ${activePage === PAGES.LOGS ? 'active' : ''}`}
            onClick={() => setActivePage(PAGES.LOGS)}
          >
            <span className="icon">📋</span>
            <span>Log Stream</span>
          </button>
          <button
            className={`nav-link ${activePage === PAGES.ALERTS ? 'active' : ''}`}
            onClick={() => setActivePage(PAGES.ALERTS)}
          >
            <span className="icon">🚨</span>
            <span>Alerts</span>
          </button>
        </nav>

        <div className="sidebar-footer">
          <span className="status-dot"></span>
          <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>System Online</span>
        </div>
      </aside>

      {/* Main Content */}
      <main className="main-content animate-in">
        {renderPage()}
      </main>
    </div>
  );
}

export default App;
