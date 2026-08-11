import { api } from './api.js';
import { byId, setServerStatus, updateText } from './ui.js';

export function initDashboard({ onError = () => {} } = {}) {
  let healthTimer;
  let metricsTimer;

  async function loadHealth() {
    try {
      const health = await api.get('/api/health');
      setServerStatus(true);
      updateHealthCard('health-api', health.status || 'UP', true);
      updateHealthCard('health-db', health.database || 'UP', health.database === 'UP');
      updateText('health-companies', health.companies || 0);
      updateText('health-tasks', health.tasks || 0);
    } catch (error) {
      setServerStatus(false);
      updateHealthCard('health-api', 'OFF', false);
      updateHealthCard('health-db', 'OFF', false);
    }
  }

  async function loadMetrics() {
    try {
      const dashboard = await api.get('/api/dashboard');
      updateText('dashboard-companies', dashboard.companies || 0);
      updateText('dashboard-people', dashboard.people || 0);
      updateText('dashboard-relationships', dashboard.relationships || 0);
      updateText('dashboard-watchlisted', dashboard.watchlisted || 0);
      updateText('dashboard-audit', dashboard.auditEvents || 0);
      updateText('dashboard-imports', dashboard.importRuns || 0);
    } catch (error) {
      ['dashboard-companies', 'dashboard-people', 'dashboard-relationships',
        'dashboard-watchlisted', 'dashboard-audit', 'dashboard-imports']
        .forEach(id => updateText(id, '-'));
      onError('Dashboard chyba', 'Metriky dashboardu se nepodarilo nacist.', 'warning');
    }
  }

  function start() {
    loadHealth();
    loadMetrics();
    healthTimer = window.setInterval(loadHealth, 30000);
    metricsTimer = window.setInterval(loadMetrics, 30000);
  }

  function stop() {
    window.clearInterval(healthTimer);
    window.clearInterval(metricsTimer);
  }

  function updateHealthCard(id, value, ok) {
    const element = byId(id);
    element.textContent = value;
    const card = element.closest('.health-card');
    card.classList.toggle('ok', ok);
    card.classList.toggle('fail', !ok);
  }

  return { start, stop, loadHealth, loadMetrics };
}
