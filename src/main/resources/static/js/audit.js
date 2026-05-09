const ARCHIVED_AUDIT_IDS_KEY = 'archivedAuditIds';
const ARCHIVED_AUDIT_ENTRIES_KEY = 'archivedAuditEntries';

export function initAudit({ dom, state, ui }) {
  state.localAuditCounter = 0;
  state.archivedAuditIds = new Set(ui.loadStoredArray(ARCHIVED_AUDIT_IDS_KEY));
  state.auditEntries = state.archivedAuditIds.has('local:startup') ? [] : [
    {
      id: 'local:startup',
      title: 'System pripraven',
      message: 'Ceka na import nebo vyhledavani firemnich dat.',
      level: 'info',
      time: 'po startu'
    }
  ];
  state.archivedAuditEntries = ui.loadStoredArray(ARCHIVED_AUDIT_ENTRIES_KEY);

  dom.auditFilter.addEventListener('change', renderAuditLog);

  dom.refreshAudit.addEventListener('click', async () => {
    await loadBackendAudit();
  });

  dom.archiveAudit.addEventListener('click', () => {
    if (!state.auditEntries.length) {
      ui.showToast('Audit log nema zadne aktivni zaznamy.');
      return;
    }
    state.auditEntries.forEach(entry => state.archivedAuditIds.add(entry.id));
    state.archivedAuditEntries = [...state.auditEntries, ...state.archivedAuditEntries];
    state.auditEntries = [];
    saveArchivedAuditState();
    state.auditArchiveVisible = true;
    dom.archiveBox.hidden = false;
    dom.showArchive.textContent = 'Skrýt archiv';
    ui.showToast('Aktivni audit log byl presunut do archivu.');
    renderAuditLog();
  });

  dom.showArchive.addEventListener('click', () => {
    state.auditArchiveVisible = !state.auditArchiveVisible;
    dom.archiveBox.hidden = !state.auditArchiveVisible;
    dom.showArchive.textContent = state.auditArchiveVisible ? 'Skrýt archiv' : 'Zobrazit archiv';
  });

  dom.printAudit.addEventListener('click', () => {
    const archiveWasHidden = dom.archiveBox.hidden;
    dom.archiveBox.hidden = false;
    document.body.classList.add('print-audit');
    window.print();
    window.setTimeout(() => {
      document.body.classList.remove('print-audit');
      dom.archiveBox.hidden = archiveWasHidden;
    }, 300);
  });

  function addActivity(title, message, level = 'info') {
    const time = new Date().toLocaleTimeString('cs-CZ', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    state.auditEntries.unshift({ id: createLocalAuditId(), title, message, level, time });
    renderAuditLog();
  }

  async function loadBackendAudit(showMessage = true) {
    try {
      const response = await fetch('/api/audit?limit=100');
      if (!response.ok) {
        throw new Error('Audit endpoint failed');
      }
      const events = await response.json();
      state.auditEntries = events.map(event => ({
        id: createBackendAuditId(event),
        title: event.type || 'AUDIT',
        message: `${event.companyName || 'Firma'} (${event.registrationNumber || '-'}) - ${event.description || ''}`,
        level: mapSeverity(event.severity),
        time: ui.formatDate(event.createdAt) || 'bez data'
      })).filter(entry => !state.archivedAuditIds.has(entry.id));
      renderAuditLog();
      if (showMessage) {
        ui.showToast('Audit log nacten z backendu.');
      }
    } catch (error) {
      addActivity('Audit chyba', 'Backendovy audit log se nepodarilo nacist.', 'warning');
    }
  }

  function createBackendAuditId(event) {
    if (event.id !== undefined && event.id !== null) {
      return `backend:${event.id}`;
    }
    return `backend:${event.type || ''}:${event.companyId || ''}:${event.createdAt || ''}:${event.description || ''}`;
  }

  function createLocalAuditId() {
    state.localAuditCounter += 1;
    return `local:${Date.now()}:${state.localAuditCounter}`;
  }

  function saveArchivedAuditState() {
    ui.storeArray(ARCHIVED_AUDIT_IDS_KEY, Array.from(state.archivedAuditIds));
    ui.storeArray(ARCHIVED_AUDIT_ENTRIES_KEY, state.archivedAuditEntries);
  }

  function renderAuditLog() {
    const filter = dom.auditFilter.value;
    const visibleEntries = state.auditEntries.filter(entry => filter === 'all' || entry.level === filter);
    const recentEntries = state.auditEntries.slice(0, 2);

    dom.activityLog.innerHTML = recentEntries.length
      ? recentEntries.map(entry => renderPreviewEntry(entry)).join('')
      : '<div class="activity-item"><strong>Audit zatim prazdny</strong><span>Aktivni log je prazdny.</span></div>';

    dom.auditList.innerHTML = visibleEntries.length
      ? visibleEntries.map(entry => renderAuditEntry(entry)).join('')
      : '<div class="empty">Pro zvoleny filtr nejsou zadne aktivni udalosti.</div>';

    dom.archiveList.innerHTML = state.archivedAuditEntries.length
      ? state.archivedAuditEntries.map(entry => renderAuditEntry(entry)).join('')
      : '<div class="empty">Archiv je zatim prazdny.</div>';

    dom.auditTotal.textContent = state.auditEntries.length;
    dom.auditCritical.textContent = state.auditEntries.filter(entry => entry.level === 'critical').length;
    dom.auditWarning.textContent = state.auditEntries.filter(entry => entry.level === 'warning').length;
    dom.auditArchived.textContent = state.archivedAuditEntries.length;
  }

  function renderPreviewEntry(entry) {
    return `
      <div class="activity-item">
        <strong>${ui.escapeHtml(entry.title)}</strong>
        <span>${ui.escapeHtml(levelLabel(entry.level))}</span>
      </div>
    `;
  }

  function renderAuditEntry(entry) {
    return `
      <div class="audit-entry ${ui.escapeHtml(entry.level)}">
        <div>
          <strong>${ui.escapeHtml(entry.title)}</strong>
          <span class="audit-time">${ui.escapeHtml(entry.time)}</span>
          <small>${ui.escapeHtml(levelLabel(entry.level))}</small>
        </div>
        <span>${ui.escapeHtml(entry.message)}</span>
      </div>
    `;
  }

  function levelLabel(level) {
    const labels = {
      low: 'nizke',
      info: 'informacni',
      warning: 'upozorneni',
      critical: 'kriticke'
    };
    return labels[level] || 'informacni';
  }

  function mapSeverity(value) {
    const normalized = String(value || '').toLowerCase();
    if (normalized === 'critical') {
      return 'critical';
    }
    if (normalized === 'warning') {
      return 'warning';
    }
    return 'info';
  }

  return {
    addActivity,
    loadBackendAudit,
    renderAuditLog
  };
}
