import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const dashboardApi = {
  getStats: () => api.get('/dashboard/stats'),
};

export const logsApi = {
  getLogs: (page = 0, size = 50) => api.get(`/logs?page=${page}&size=${size}`),
  getSources: () => api.get('/logs/sources'),
  getCount: () => api.get('/logs/count'),
};

export const alertsApi = {
  getAlerts: (page = 0, size = 50) => api.get(`/alerts?page=${page}&size=${size}`),
  getActiveAlerts: () => api.get('/alerts/active'),
  resolveAlert: (id) => api.put(`/alerts/${id}/resolve`),
  getActiveCount: () => api.get('/alerts/count/active'),
};

export default api;
