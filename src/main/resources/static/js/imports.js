import { api } from './api.js';
import { byId, escapeHtml, formatDate, setButtonBusy, showToast, updateText } from './ui.js';

export function initImports({ audit, demoCompanies, sampleCsv, sampleJson, onChanged = async () => {} }) {
  const jsonInput = byId('json-input');
  const csvInput = byId('csv-input');
  const previewBlock = byId('import-preview-block');
  const preview = byId('import-preview');
  const runs = byId('import-runs');
  const runDetail = byId('import-run-detail');

  function init() {
    jsonInput.value = sampleJson;
    csvInput.value = sampleCsv;
    renderDemoList();
    initTabs();
    byId('load-json-demo').addEventListener('click', () => setSample(jsonInput, sampleJson, 'JSON ukazka je pripravena.'));
    byId('load-csv-demo').addEventListener('click', () => setSample(csvInput, sampleCsv, 'CSV ukazka je pripravena.'));
    byId('preview-json').addEventListener('click', event => previewPayload(event.currentTarget, '/api/import/preview/json', jsonInput.value, 'application/json'));
    byId('preview-csv').addEventListener('click', event => previewPayload(event.currentTarget, '/api/import/preview/csv', csvInput.value, 'text/csv'));
    byId('import-json').addEventListener('click', event => importPayload(event.currentTarget, '/api/import/json', jsonInput.value, 'application/json', 'JSON import dokoncen.'));
    byId('import-csv').addEventListener('click', event => importPayload(event.currentTarget, '/api/import/csv', csvInput.value, 'text/csv', 'CSV import dokoncen.'));
    byId('ares-form').addEventListener('submit', event => {
      event.preventDefault();
      importAres(byId('ico').value, event.submitter);
    });
    byId('seed-demo').addEventListener('click', seedDemo);
    byId('demo-list').addEventListener('click', event => {
      const button = event.target.closest('button[data-ico]');
      if (button) importAres(button.dataset.ico, button);
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

  function setSample(input, value, message) {
    input.value = value;
    showToast(message);
  }

  function renderDemoList() {
    byId('demo-list').innerHTML = demoCompanies.map(item => `
      <div class="demo-item"><div><strong>${escapeHtml(item.name)}</strong><span>ICO ${item.ico}</span></div>
        <button class="secondary" type="button" data-ico="${item.ico}">Nahrat</button></div>`).join('');
  }

  async function importAres(value, button) {
    const normalized = String(value || '').replace(/\D/g, '');
    if (!normalized) {
      showToast('Zadej platne ICO.');
      audit.addActivity('Neplatny vstup', 'ARES import byl zastaven, protoze ICO nebylo vyplneno.', 'warning');
      return;
    }
    if (button) setButtonBusy(button, true, 'Importuji...');
    try {
      const company = await api.send(`/api/import/ares/${encodeURIComponent(normalized)}`, 'POST', null, 'application/json');
      showToast(`Nahrano: ${company.name}`);
      audit.addActivity('Import z ARES', `ICO ${normalized} ulozeno jako ${company.name}.`, 'info');
      await onChanged();
      await loadRuns(false);
    } catch (error) {
      showToast(`ARES import selhal pro ICO ${normalized}.`);
      audit.addActivity('ARES chyba', `Import ICO ${normalized} selhal.`, 'critical');
      await audit.load(false);
    } finally {
      if (button) setButtonBusy(button, false);
    }
  }

  async function seedDemo(event) {
    const button = event.currentTarget;
    setButtonBusy(button, true, 'Nahravam demo...');
    let imported = 0;
    for (const company of demoCompanies) {
      try {
        await api.send(`/api/import/ares/${company.ico}`, 'POST', null, 'application/json');
        imported += 1;
      } catch (error) {
        audit.addActivity('ARES chyba', `Demo import ICO ${company.ico} selhal.`, 'warning');
      }
    }
    setButtonBusy(button, false);
    showToast(`Demo import dokoncen: ${imported}/${demoCompanies.length}.`);
    audit.addActivity('Import z ARES', `${imported} ukazkove subjekty byly nacteny pres ARES API.`, 'info');
    await onChanged();
    await loadRuns(false);
  }

  async function importPayload(button, url, body, contentType, successMessage) {
    if (!String(body || '').trim()) {
      showToast('Nejdrive vloz data pro import.');
      return;
    }
    setButtonBusy(button, true, 'Importuji...');
    try {
      const result = await api.send(url, 'POST', body, contentType);
      showToast(`${successMessage} Ulozeno: ${result.imported || 0}, chyby: ${result.failed || 0}.`);
      audit.addActivity('Import dat', `${result.imported || 0} zaznamu ulozeno do databaze.`, result.failed ? 'warning' : 'info');
      await onChanged();
      await loadRuns(false);
      showTab('history');
    } catch (error) {
      showToast('Import selhal. Zkontroluj format vstupu.');
      audit.addActivity('Chyba importu', 'Import selhal pri kontrole formatu vstupnich dat.', 'critical');
      await audit.load(false);
    } finally {
      setButtonBusy(button, false);
    }
  }

  async function previewPayload(button, url, body, contentType) {
    if (!String(body || '').trim()) {
      showToast('Nejdrive vloz data pro kontrolu.');
      return;
    }
    setButtonBusy(button, true, 'Kontroluji...');
    try {
      const result = await api.send(url, 'POST', body, contentType);
      renderPreview(result);
      showToast(result.invalidRows ? 'Kontrola nasla chyby ve vstupu.' : 'Vstup je pripraveny k importu.');
    } catch (error) {
      showToast('Kontrola vstupu selhala.');
      audit.addActivity('Kontrola importu', 'Backend nevratil validacni nahled importu.', 'warning');
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
      if (showMessage) showToast('Historie importu nactena z backendu.');
    } catch (error) {
      runs.innerHTML = '<div class="empty">Historii importu se nepodarilo nacist.</div>';
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
      showToast('Detail importu se nepodarilo nacist.');
    }
  }

  function renderRuns(importRuns) {
    runs.innerHTML = importRuns.length ? importRuns.map(run => `
      <article class="import-run"><div class="import-run-summary">
        <strong>${escapeHtml(run.sourceType || 'IMPORT')} · ${escapeHtml(run.status || 'UNKNOWN')}</strong>
        <span>${formatDate(run.startedAt)}</span><div class="meta"><span>${run.importedRows || 0} ulozeno</span><span>${run.failedRows || 0} chyb</span></div>
      </div><button class="secondary" type="button" data-import-run-id="${run.id}">Detail</button></article>`).join('')
      : '<div class="empty">Zatim neprobehly zadne importy.</div>';
  }

  function renderRunDetail(run) {
    const errors = run.errors || [];
    runDetail.innerHTML = `
      <div class="import-run-detail-head"><div><h3>${escapeHtml(run.sourceType || 'IMPORT')} · ${escapeHtml(run.status || 'UNKNOWN')}</h3>
        <p>Beh #${run.id || '-'} · ${formatDate(run.startedAt)}</p></div>
        <button class="secondary" type="button" id="back-to-import-runs">Zpet na historii</button></div>
      <div class="import-run-detail-grid">
        <div class="import-run-detail-card"><span>Radku celkem</span><strong>${run.totalRows || 0}</strong></div>
        <div class="import-run-detail-card"><span>Ulozeno</span><strong>${run.importedRows || 0}</strong></div>
        <div class="import-run-detail-card"><span>Chybne</span><strong>${run.failedRows || 0}</strong></div>
        <div class="import-run-detail-card"><span>Zdroj</span><strong>${escapeHtml(run.sourceType || '-')}</strong></div>
      </div><div class="import-run-errors">${errors.length ? errors.map(error => `
        <div class="import-run-error"><strong>Radek ${error.rowNumber || '-'}</strong><span>${escapeHtml(error.message || '')}</span>
          ${error.rawValue ? `<code>${escapeHtml(error.rawValue)}</code>` : ''}</div>`).join('')
        : '<div class="empty">Import neobsahuje zadne chybove radky.</div>'}</div>`;
  }

  function renderPreview(result) {
    const rows = result.rows || [];
    preview.innerHTML = `
      <div class="import-preview-header">
        <div class="import-preview-stat"><span>Zdroj</span><strong>${escapeHtml(result.sourceType || 'IMPORT')}</strong></div>
        <div class="import-preview-stat"><span>Radku</span><strong>${result.totalRows || 0}</strong></div>
        <div class="import-preview-stat"><span>Pripraveno</span><strong>${result.validRows || 0}</strong></div>
        <div class="import-preview-stat"><span>Chyby</span><strong>${result.invalidRows || 0}</strong></div>
      </div>${rows.length ? rows.map(row => `
        <article class="import-preview-row ${row.valid ? 'valid' : 'invalid'}"><strong>Radek ${row.rowNumber || '-'} · ${row.valid ? 'pripraveno' : 'chyba'}</strong>
          <span>${escapeHtml(row.name || 'bez nazvu')} ${row.registrationNumber ? `· ICO ${escapeHtml(row.registrationNumber)}` : ''}</span>
          <span>${escapeHtml(row.message || '')}</span>${!row.valid && row.rawValue ? `<code>${escapeHtml(row.rawValue)}</code>` : ''}</article>`).join('')
        : '<div class="empty">Kontrola neobsahuje zadne datove radky.</div>'}`;
  }

  function updateSummary(importRuns) {
    updateText('import-total-runs', importRuns.length);
    updateText('import-total-saved', importRuns.reduce((sum, run) => sum + (run.importedRows || 0), 0));
    updateText('import-total-failed', importRuns.reduce((sum, run) => sum + (run.failedRows || 0), 0));
    updateText('import-last-status', importRuns.length ? importRuns[0].status || '-' : '-');
  }

  return { init, loadRuns };
}
