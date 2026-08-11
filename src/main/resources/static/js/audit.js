import { api } from './api.js';
import { byId, escapeHtml, formatDate, showToast, updateText } from './ui.js';

export function initAudit() {
  const controls = {
    type: byId('audit-type-filter'),
    severity: byId('audit-filter'),
    query: byId('audit-query-filter'),
    importId: byId('audit-import-filter'),
    from: byId('audit-from-filter'),
    to: byId('audit-to-filter')
  };
  const list = byId('audit-list');
  const archiveList = byId('archive-list');
  const archiveBox = byId('archive-box');
  const showArchive = byId('show-archive');
  const activityLog = byId('activity-log');
  let backendEntries = [];
  let archivedEntries = [];
  let localEntries = [{
    id: null,
    title: 'System pripraven',
    type: 'CLIENT',
    message: 'Ceka na import nebo vyhledavani firemnich dat.',
    level: 'info',
    time: 'po startu',
    archived: false
  }];
  let archiveVisible = false;

  function init() {
    controls.type.addEventListener('change', () => load(false));
    controls.severity.addEventListener('change', () => load(false));
    controls.query.addEventListener('keydown', event => {
      if (event.key === 'Enter') {
        event.preventDefault();
        load(false);
      }
    });
    [controls.importId, controls.from, controls.to].forEach(control => {
      control.addEventListener('change', () => load(false));
    });
    byId('refresh-audit').addEventListener('click', () => load(true));
    byId('clear-audit-filters').addEventListener('click', clearFilters);
    byId('archive-audit').addEventListener('click', archiveActive);
    byId('export-audit').addEventListener('click', exportCsv);
    byId('print-audit').addEventListener('click', print);
    showArchive.addEventListener('click', toggleArchive);
    list.addEventListener('click', handleEntryAction);
    archiveList.addEventListener('click', handleEntryAction);
    render();
  }

  function addActivity(title, message, level = 'info') {
    localEntries.unshift({
      id: null,
      title,
      type: 'CLIENT',
      message,
      level,
      time: new Date().toLocaleTimeString('cs-CZ', {
        hour: '2-digit', minute: '2-digit', second: '2-digit'
      }),
      archived: false
    });
    localEntries = localEntries.slice(0, 20);
    render();
  }

  async function load(showMessage = true) {
    try {
      const [active, archived] = await Promise.all([
        api.get(`/api/audit?${buildQuery(false, true)}`),
        api.get(`/api/audit?${buildQuery(true, true)}`)
      ]);
      backendEntries = active.map(toEntry);
      archivedEntries = archived.map(toEntry);
      render();
      if (showMessage) {
        showToast('Audit log nacten z backendu.');
      }
    } catch (error) {
      addActivity('Audit chyba', 'Backendovy audit log se nepodarilo nacist.', 'warning');
    }
  }

  async function loadTypes() {
    try {
      const current = controls.type.value;
      const types = await api.get('/api/audit/types');
      controls.type.innerHTML = '<option value="">Vsechny typy udalosti</option>'
        + types.map(type => `<option value="${escapeHtml(type)}">${escapeHtml(type)}</option>`).join('');
      controls.type.value = types.includes(current) ? current : '';
    } catch (error) {
      addActivity('Audit typy', 'Typy auditnich udalosti se nepodarilo nacist.', 'warning');
    }
  }

  function buildQuery(archived, includeLimit) {
    const params = new URLSearchParams({ archived: String(archived) });
    if (includeLimit) params.set('limit', '200');
    if (controls.type.value) params.set('type', controls.type.value);
    if (controls.severity.value !== 'all') params.set('severity', controls.severity.value);
    if (controls.query.value.trim()) params.set('query', controls.query.value.trim());
    if (controls.importId.value) params.set('importRunId', controls.importId.value);
    if (controls.from.value) params.set('from', controls.from.value);
    if (controls.to.value) params.set('to', controls.to.value);
    return params.toString();
  }

  async function clearFilters() {
    controls.type.value = '';
    controls.severity.value = 'all';
    controls.query.value = '';
    controls.importId.value = '';
    controls.from.value = '';
    controls.to.value = '';
    await load(false);
  }

  function exportCsv() {
    const link = document.createElement('a');
    link.href = `/api/audit/export.csv?${buildQuery(false, false)}`;
    link.download = 'company-intelligence-audit.csv';
    document.body.appendChild(link);
    link.click();
    link.remove();
  }

  async function archiveActive() {
    const ids = backendEntries.map(entry => entry.id).filter(Number.isFinite);
    if (!ids.length) {
      showToast('Audit log nema zadne aktivni zaznamy.');
      return;
    }
    try {
      await api.post('/api/audit/archive', { ids, archived: true });
      archiveVisible = true;
      syncArchiveVisibility();
      showToast('Aktivni audit log byl presunut do archivu.');
      await load(false);
    } catch (error) {
      showToast('Archivace audit logu selhala.');
      addActivity('Audit archiv', 'Backend neulozil archivaci vybranych udalosti.', 'warning');
    }
  }

  async function handleEntryAction(event) {
    const archiveButton = event.target.closest('button[data-audit-archive]');
    const restoreButton = event.target.closest('button[data-audit-restore]');
    const button = archiveButton || restoreButton;
    if (!button) return;

    const id = Number(button.dataset.auditArchive || button.dataset.auditRestore);
    try {
      await api.post(`/api/audit/${id}/archive`, { archived: Boolean(archiveButton) });
      showToast(archiveButton ? 'Udalost byla archivovana.' : 'Udalost byla obnovena.');
      await load(false);
    } catch (error) {
      showToast('Stav auditni udalosti se nepodarilo ulozit.');
    }
  }

  function toggleArchive() {
    archiveVisible = !archiveVisible;
    syncArchiveVisibility();
  }

  function syncArchiveVisibility() {
    archiveBox.hidden = !archiveVisible;
    showArchive.textContent = archiveVisible ? 'Skryt archiv' : 'Zobrazit archiv';
  }

  function print() {
    const wasHidden = archiveBox.hidden;
    archiveBox.hidden = false;
    document.body.classList.add('print-audit');
    window.print();
    window.setTimeout(() => {
      document.body.classList.remove('print-audit');
      archiveBox.hidden = wasHidden;
    }, 300);
  }

  function toEntry(event) {
    const subject = event.importRunId
      ? `Import #${event.importRunId}`
      : `${event.companyName || 'Firma'} (${event.registrationNumber || '-'})`;
    return {
      id: event.id,
      title: event.type || 'AUDIT',
      type: event.type || 'AUDIT',
      message: `${subject} - ${event.description || ''}`,
      level: mapSeverity(event.severity),
      time: formatDate(event.createdAt) || 'bez data',
      archived: Boolean(event.archived)
    };
  }

  function render() {
    const activeEntries = [...localEntries, ...backendEntries];
    const recent = activeEntries.slice(0, 2);
    activityLog.innerHTML = recent.length
      ? recent.map(renderPreviewEntry).join('')
      : '<div class="activity-item"><strong>Audit zatim prazdny</strong><span>Aktivni log je prazdny.</span></div>';
    list.innerHTML = activeEntries.length
      ? activeEntries.map(renderEntry).join('')
      : '<div class="empty">Pro zvoleny filtr nejsou zadne aktivni udalosti.</div>';
    archiveList.innerHTML = archivedEntries.length
      ? archivedEntries.map(renderEntry).join('')
      : '<div class="empty">Archiv je zatim prazdny.</div>';
    updateText('audit-total', activeEntries.length);
    updateText('audit-critical', activeEntries.filter(entry => entry.level === 'critical').length);
    updateText('audit-warning', activeEntries.filter(entry => entry.level === 'warning').length);
    updateText('audit-archived', archivedEntries.length);
  }

  function renderPreviewEntry(entry) {
    return `<div class="activity-item activity-${escapeHtml(entry.level)}">
      <strong>${escapeHtml(entry.title)}</strong><span>${escapeHtml(levelLabel(entry.level))}</span>
    </div>`;
  }

  function renderEntry(entry) {
    const action = entry.archived
      ? `<button class="secondary audit-action" type="button" data-audit-restore="${entry.id}">Obnovit</button>`
      : `<button class="secondary audit-action" type="button" data-audit-archive="${entry.id}">Archivovat</button>`;
    return `<div class="audit-entry ${escapeHtml(entry.level)}">
      <div><strong>${escapeHtml(entry.title)}</strong><span class="audit-time">${escapeHtml(entry.time)}</span>
        <small>${escapeHtml(levelLabel(entry.level))}</small></div>
      <span>${escapeHtml(entry.message)}</span>${Number.isFinite(entry.id) ? action : ''}
    </div>`;
  }

  function levelLabel(level) {
    return ({ low: 'nizke', info: 'informacni', warning: 'upozorneni', critical: 'kriticke' })[level]
      || 'informacni';
  }

  function mapSeverity(value) {
    const severity = String(value || '').toLowerCase();
    return ['critical', 'warning', 'low'].includes(severity) ? severity : 'info';
  }

  return { init, load, loadTypes, addActivity };
}
