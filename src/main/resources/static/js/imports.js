export function initImports({ dom, state, ui, audit, companies, demoCompanies, sampleCsv, sampleJson }) {
  dom.demoList.innerHTML = demoCompanies.map(item => `
    <div class="demo-item">
      <div>
        <strong>${ui.escapeHtml(item.name)}</strong>
        <span>ICO ${item.ico}</span>
      </div>
      <button class="secondary" type="button" data-ico="${item.ico}">Nahrat</button>
    </div>
  `).join('');

  dom.demoList.addEventListener('click', event => {
    const button = event.target.closest('button[data-ico]');
    if (button) {
      importAres(button.dataset.ico);
    }
  });

  dom.jsonInput.value = sampleJson;
  dom.csvInput.value = sampleCsv;

  document.getElementById('load-json-demo').addEventListener('click', () => {
    dom.jsonInput.value = sampleJson;
    ui.showToast('JSON ukazka je pripravena.');
  });

  document.getElementById('load-csv-demo').addEventListener('click', () => {
    dom.csvInput.value = sampleCsv;
    ui.showToast('CSV ukazka je pripravena.');
  });

  document.getElementById('import-json').addEventListener('click', async () => {
    await importPayload('/api/import/json', dom.jsonInput.value, 'application/json', 'JSON import dokoncen.');
  });

  document.getElementById('import-csv').addEventListener('click', async () => {
    await importPayload('/api/import/csv', dom.csvInput.value, 'text/csv', 'CSV import dokoncen.');
  });

  document.getElementById('seed-demo').addEventListener('click', async () => {
    ui.showToast('Nacitam demo data z ARES...');
    for (const item of demoCompanies) {
      await importAres(item.ico, false);
    }
    ui.showToast('Demo data jsou nahrana.');
    audit.addActivity('Import z ARES', `${demoCompanies.length} ukazkove subjekty byly nacteny pres ARES API.`, 'info');
    await companies.search('');
  });

  dom.aresForm.addEventListener('submit', async event => {
    event.preventDefault();
    await importAres(dom.ico.value);
  });

  async function importAres(value, refresh = true) {
    const normalized = String(value || '').replace(/\D/g, '');
    if (!normalized) {
      ui.showToast('Zadej platne ICO.');
      audit.addActivity('Neplatny vstup', 'ARES import byl zastaven, protoze ICO nebylo vyplneno.', 'warning');
      return;
    }

    ui.showToast(`Importuji ICO ${normalized} z ARES...`);
    const response = await fetch(`/api/import/ares/${encodeURIComponent(normalized)}`, { method: 'POST' });
    if (!response.ok) {
      ui.showToast(`ARES import selhal pro ICO ${normalized}.`);
      audit.addActivity('ARES chyba', `Import ICO ${normalized} selhal.`, 'critical');
      await audit.loadBackendAudit(false);
      return;
    }
    const company = await response.json();
    ui.showToast(`Nahrano: ${company.name}`);
    audit.addActivity('Import z ARES', `ICO ${normalized} ulozeno jako ${company.name}.`, 'info');
    await audit.loadBackendAudit(false);
    if (refresh) {
      dom.query.value = '';
      state.watchlistOnly = false;
      dom.showWatchlist.classList.remove('active');
      await companies.search('');
    }
  }

  async function importPayload(url, body, contentType, successMessage) {
    if (!String(body || '').trim()) {
      ui.showToast('Nejdrive vloz data pro import.');
      audit.addActivity('Prazdny import', 'Import byl zastaven, protoze vstup neobsahoval data.', 'warning');
      return;
    }

    ui.showToast('Importuji data...');
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': contentType },
      body
    });
    if (!response.ok) {
      ui.showToast('Import selhal. Zkontroluj format vstupu.');
      audit.addActivity('Chyba importu', 'Import selhal pri kontrole formatu vstupnich dat.', 'critical');
      await audit.loadBackendAudit(false);
      return;
    }
    const result = await response.json();
    ui.showToast(successMessage);
    audit.addActivity('Import dat', `${result.imported || 0} zaznamu ulozeno do databaze.`, 'info');
    await audit.loadBackendAudit(false);
    dom.query.value = '';
    state.watchlistOnly = false;
    dom.showWatchlist.classList.remove('active');
    await companies.search('');
  }
}
