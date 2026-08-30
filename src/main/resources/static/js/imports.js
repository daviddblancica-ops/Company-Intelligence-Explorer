import { api } from './api.js';
import { byId, escapeHtml, formatDate, setButtonBusy, showToast, updateText } from './ui.js';

export function initImports({ audit, onChanged = async () => {} }) {
  const jsonInput = byId('json-input');
  const csvInput = byId('csv-input');
  const previewBlock = byId('import-preview-block');
  const preview = byId('import-preview');
  const runs = byId('import-runs');
  const runDetail = byId('import-run-detail');

  function init() {
    initTabs();
    byId('preview-json').addEventListener('click', event => previewPayload(event.currentTarget, '/api/import/preview/json', jsonInput.value, 'application/json'));
    byId('preview-csv').addEventListener('click', event => previewPayload(event.currentTarget, '/api/import/preview/csv', csvInput.value, 'text/csv'));
    byId('import-json').addEventListener('click', event => importPayload(event.currentTarget, '/api/import/json', jsonInput.value, 'application/json', 'JSON import dokončen.'));
    byId('import-csv').addEventListener('click', event => importPayload(event.currentTarget, '/api/import/csv', csvInput.value, 'text/csv', 'CSV import dokončen.'));
    byId('ares-form').addEventListener('submit', event => {
      event.preventDefault();
      importAres(byId('ico').value, event.submitter);
    });
    byId('refresh-import-runs').addEventListener('click', () => loadRuns(true));
    runs.addEventListener('click', event => {
      const button = event.target.closest('[data-import-run-id]');
      if (button) loadRunDetail(button.dataset.importRunId);
    });
    runDetail.addEventListener('click', event => {
      if (!event.target.closest('#back-to-import-runs')) return;
      runDetail.hidden = true;
      runs.hidden = false;
    });
    if (document.body.dataset.access === 'viewer') showTab('history');
  }

  function initTabs() {
    const tabs = Array.from(document.querySelectorAll('[data-import-tab]'));
    const panes = Array.from(document.querySelectorAll('[data-import-pane]'));
    tabs.forEach(tab => tab.addEventListener('click', () => {
      const target = tab.dataset.importTab;
      tabs.forEach(item => {
        const active = item === tab;
        item.classList.toggle('active', active);
        item.setAttribute('aria-selected', String(active));
      });
      panes.forEach(pane => pane.hidden = pane.dataset.importPane !== target);
      previewBlock.hidden = !['json', 'csv'].includes(target);
      if (target === 'history') loadRuns(false);
    }));
  }

  async function importAres(value, button) {
    const normalized = String(value || '').replace(/\D/g, '');
    if (!normalized) {
      showToast('Zadej platné IČO.');
      audit.addActivity('Neplatný vstup', 'Import z ARES byl zastaven, protože IČO nebylo vyplněno.', 'warning');
      return;
    }
    if (button) setButtonBusy(button, true, 'Importuji...');
    try {
      const company = await api.send(`/api/import/ares/${encodeURIComponent(normalized)}`, 'POST', null, 'application/json');
      showToast(`Nahráno: ${company.name}`);
      audit.addActivity('Import z ARES', `IČO ${normalized} uloženo jako ${company.name}.`, 'info');
      await onChanged();
      await loadRuns(false);
    } catch (error) {
      showToast(`Import z ARES selhal pro IČO ${normalized}.`);
      audit.addActivity('Chyba ARES', `Import IČO ${normalized} selhal.`, 'critical');
      await audit.load(false);
    } finally {
      if (button) setButtonBusy(button, false);
    }
  }

  async function importPayload(button, url, body, contentType, successMessage) {
    if (!String(body || '').trim()) {
      showToast('Nejdříve vlož data pro import.');
      return;
    }
    setButtonBusy(button, true, 'Importuji...');
    try {
      const result = await api.send(url, 'POST', body, contentType);
      showToast(`${successMessage} Uloženo: ${result.imported || 0}, chyby: ${result.failed || 0}.`);
      audit.addActivity('Import dat', `${result.imported || 0} záznamů uloženo do databáze.`, result.failed ? 'warning' : 'info');
      await onChanged();
      await loadRuns(false);
      showTab('history');
    } catch (error) {
      showToast(error.message || 'Import selhal. Zkontroluj formát vstupu.');
      audit.addActivity('Chyba importu', 'Import selhal při kontrole formátu vstupních dat.', 'critical');
      await audit.load(false);
    } finally {
      setButtonBusy(button, false);
    }
  }

  async function previewPayload(button, url, body, contentType) {
    if (!String(body || '').trim()) {
      showToast('Nejdříve vlož data pro kontrolu.');
      return;
    }
    setButtonBusy(button, true, 'Kontroluji...');
    try {
      const result = await api.send(url, 'POST', body, contentType);
      renderPreview(result);
      showToast(result.invalidRows ? 'Kontrola našla chyby ve vstupu.' : 'Vstup je připravený k importu.');
    } catch (error) {
      showToast(error.message || 'Kontrola vstupu selhala.');
      audit.addActivity('Kontrola importu', 'Backend nevrátil validační náhled importu.', 'warning');
    } finally {
      setButtonBusy(button, false);
    }
  }

  function showTab(name) {
    const tab = document.querySelector(`[data-import-tab="${name}"]`);
    if (tab) tab.click();
  }

  async function loadRuns(showMessage = false) {
    try {
      const importRuns = await api.get('/api/import/runs');
      renderRuns(importRuns);
      updateSummary(importRuns);
      runDetail.hidden = true;
      runs.hidden = false;
      if (showMessage) showToast('Historie importu načtena z backendu.');
    } catch (error) {
      runs.innerHTML = '<div class="empty">Historii importu se nepodařilo načíst.</div>';
      updateSummary([]);
    }
  }

  async function loadRunDetail(id) {
    try {
      const run = await api.get(`/api/import/runs/${encodeURIComponent(id)}`);
      renderRunDetail(run);
      runs.hidden = true;
      runDetail.hidden = false;
    } catch (error) {
      showToast('Detail importu se nepodařilo načíst.');
    }
  }

  function renderRuns(importRuns) {
    runs.innerHTML = importRuns.length ? importRuns.map(run => `
      <article class="import-run"><div class="import-run-summary">
        <strong>${escapeHtml(run.sourceType || 'IMPORT')} · ${escapeHtml(run.status || 'UNKNOWN')}</strong>
        <span>${formatDate(run.startedAt)}</span><div class="meta"><span>${run.importedRows || 0} uloženo</span><span>${run.failedRows || 0} chyb</span></div>
      </div><button class="secondary" type="button" data-import-run-id="${run.id}">Detail</button></article>`).join('')
      : '<div class="empty">Zatím neproběhly žádné importy.</div>';
  }

  function renderRunDetail(run) {
    const errors = run.errors || [];
    runDetail.innerHTML = `
      <div class="import-run-detail-head"><div><h3>${escapeHtml(run.sourceType || 'IMPORT')} · ${escapeHtml(run.status || 'UNKNOWN')}</h3>
        <p>Běh #${run.id || '-'} · ${formatDate(run.startedAt)}</p></div>
        <button class="secondary" type="button" id="back-to-import-runs">Zpět na historii</button></div>
      <div class="import-run-detail-grid">
        <div class="import-run-detail-card"><span>Řádků celkem</span><strong>${run.totalRows || 0}</strong></div>
        <div class="import-run-detail-card"><span>Uloženo</span><strong>${run.importedRows || 0}</strong></div>
        <div class="import-run-detail-card"><span>Chybné</span><strong>${run.failedRows || 0}</strong></div>
        <div class="import-run-detail-card"><span>Zdroj</span><strong>${escapeHtml(run.sourceType || '-')}</strong></div>
      </div><div class="import-run-errors">${errors.length ? errors.map(error => `
        <div class="import-run-error"><strong>Řádek ${error.rowNumber || '-'}</strong><span>${escapeHtml(error.message || '')}</span>
          ${error.rawValue ? `<code>${escapeHtml(error.rawValue)}</code>` : ''}</div>`).join('')
        : '<div class="empty">Import neobsahuje žádné chybové řádky.</div>'}</div>`;
  }

  function renderPreview(result) {
    const rows = result.rows || [];
    preview.innerHTML = `
      <div class="import-preview-header">
        <div class="import-preview-stat"><span>Zdroj</span><strong>${escapeHtml(result.sourceType || 'IMPORT')}</strong></div>
        <div class="import-preview-stat"><span>Řádků</span><strong>${result.totalRows || 0}</strong></div>
        <div class="import-preview-stat"><span>Připraveno</span><strong>${result.validRows || 0}</strong></div>
        <div class="import-preview-stat"><span>Chyby</span><strong>${result.invalidRows || 0}</strong></div>
      </div>${rows.length ? rows.map(row => `
        <article class="import-preview-row ${row.valid ? 'valid' : 'invalid'}"><strong>Řádek ${row.rowNumber || '-'} · ${row.valid ? 'připraveno' : 'chyba'}</strong>
          <span>${escapeHtml(row.name || 'bez názvu')} ${row.registrationNumber ? `· IČO ${escapeHtml(row.registrationNumber)}` : ''}</span>
          <span>${escapeHtml(row.message || '')}</span>${!row.valid && row.rawValue ? `<code>${escapeHtml(row.rawValue)}</code>` : ''}</article>`).join('')
        : '<div class="empty">Kontrola neobsahuje žádné datové řádky.</div>'}`;
  }

  function updateSummary(importRuns) {
    updateText('import-total-runs', importRuns.length);
    updateText('import-total-saved', importRuns.reduce((sum, run) => sum + (run.importedRows || 0), 0));
    updateText('import-total-failed', importRuns.reduce((sum, run) => sum + (run.failedRows || 0), 0));
    updateText('import-last-status', importRuns.length ? importRuns[0].status || '-' : '-');
  }

  return { init, loadRuns };
}
