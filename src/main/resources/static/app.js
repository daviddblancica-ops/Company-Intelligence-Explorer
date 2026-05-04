    // Demo data
    const demoCompanies = [
      { ico: '00006947', name: 'Ministerstvo financi' },
      { ico: '26168685', name: 'Seznam.cz' },
      { ico: '27082440', name: 'Alza.cz' },
      { ico: '45274649', name: 'CEZ' }
    ];

    const sampleJson = JSON.stringify([
      {
        name: 'Atlas Data Lab s.r.o.',
        registrationNumber: '70010001',
        country: 'CZ',
        legalForm: 's.r.o.',
        address: 'Na Prikope 12, Praha',
        people: [
          { fullName: 'Jan Novak', role: 'jednatel' },
          { fullName: 'Eva Svobodova', role: 'datova analyticka' }
        ]
      },
      {
        name: 'North Bridge Ventures a.s.',
        registrationNumber: '70010002',
        country: 'CZ',
        legalForm: 'a.s.',
        address: 'Jana Babaka 11, Brno',
        people: [
          { fullName: 'Petra Dvorakova', role: 'clen predstavenstva' },
          { fullName: 'Tomas Marek', role: 'financni reditel' }
        ]
      }
    ], null, 2);

    const sampleCsv = [
      'name,registrationNumber,country,legalForm,people',
      'Data Bridge s.r.o.,70020001,CZ,s.r.o.,Michaela Cerna|jednatel;Karel Novak|analytik',
      'Meridian Trade a.s.,70020002,CZ,a.s.,Lucie Hruba|clen predstavenstva;Pavel Urban|obchodni reditel'
    ].join('\n');

    // DOM references
    const form = document.getElementById('search-form');
    const query = document.getElementById('query');
    const results = document.getElementById('results');
    const detailPanel = document.getElementById('detail-panel');
    const aresForm = document.getElementById('ares-form');
    const ico = document.getElementById('ico');
    const jsonInput = document.getElementById('json-input');
    const csvInput = document.getElementById('csv-input');
    const toast = document.getElementById('toast');
    const activityLog = document.getElementById('activity-log');
    const auditList = document.getElementById('audit-list');
    const archiveList = document.getElementById('archive-list');
    const archiveBox = document.getElementById('archive-box');
    const auditFilter = document.getElementById('audit-filter');
    const refreshAudit = document.getElementById('refresh-audit');
    const archiveAudit = document.getElementById('archive-audit');
    const printAudit = document.getElementById('print-audit');
    const showArchive = document.getElementById('show-archive');
    const serverStatus = document.getElementById('server-status');
    const statusTitle = document.getElementById('status-title');
    const shell = document.getElementById('app-shell');
    const sidebarToggle = document.getElementById('sidebar-toggle');
    const demoList = document.getElementById('demo-list');
    const showAll = document.getElementById('show-all');
    const showWatchlist = document.getElementById('show-watchlist');
    const pages = Array.from(document.querySelectorAll('.page'));
    const pageLinks = Array.from(document.querySelectorAll('nav a[href^="#"]'));
    // Application state
    const ARCHIVED_AUDIT_IDS_KEY = 'archivedAuditIds';
    const ARCHIVED_AUDIT_ENTRIES_KEY = 'archivedAuditEntries';
    let currentCompanies = [];
    let watchlistOnly = false;
    let selectedCompanyId = null;
    let auditArchiveVisible = false;
    let localAuditCounter = 0;
    let archivedAuditIds = new Set(loadStoredArray(ARCHIVED_AUDIT_IDS_KEY));
    let auditEntries = archivedAuditIds.has('local:startup') ? [] : [
      {
        id: 'local:startup',
        title: 'System pripraven',
        message: 'Ceka na import nebo vyhledavani firemnich dat.',
        level: 'info',
        time: 'po startu'
      }
    ];
    let archivedAuditEntries = loadStoredArray(ARCHIVED_AUDIT_ENTRIES_KEY);

    // Bootstrap and event binding
    const sidebarCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
    setSidebarCollapsed(sidebarCollapsed);

    sidebarToggle.addEventListener('click', () => {
      setSidebarCollapsed(!shell.classList.contains('sidebar-collapsed'));
    });

    window.addEventListener('hashchange', showCurrentPage);

    demoList.innerHTML = demoCompanies.map(item => `
      <div class="demo-item">
        <div>
          <strong>${escapeHtml(item.name)}</strong>
          <span>ICO ${item.ico}</span>
        </div>
        <button class="secondary" type="button" data-ico="${item.ico}">Nahrat</button>
      </div>
    `).join('');

    demoList.addEventListener('click', event => {
      const button = event.target.closest('button[data-ico]');
      if (button) {
        importAres(button.dataset.ico);
      }
    });

    jsonInput.value = sampleJson;
    csvInput.value = sampleCsv;

    document.getElementById('load-json-demo').addEventListener('click', () => {
      jsonInput.value = sampleJson;
      showToast('JSON ukazka je pripravena.');
    });

    document.getElementById('load-csv-demo').addEventListener('click', () => {
      csvInput.value = sampleCsv;
      showToast('CSV ukazka je pripravena.');
    });

    document.getElementById('import-json').addEventListener('click', async () => {
      await importPayload('/api/import/json', jsonInput.value, 'application/json', 'JSON import dokoncen.');
    });

    document.getElementById('import-csv').addEventListener('click', async () => {
      await importPayload('/api/import/csv', csvInput.value, 'text/csv', 'CSV import dokoncen.');
    });

    document.getElementById('seed-demo').addEventListener('click', async () => {
      showToast('Nacitam demo data z ARES...');
      for (const item of demoCompanies) {
        await importAres(item.ico, false);
      }
      showToast('Demo data jsou nahrana.');
      addActivity('Import z ARES', `${demoCompanies.length} ukazkove subjekty byly nacteny pres ARES API.`, 'info');
      await search('');
    });

    aresForm.addEventListener('submit', async event => {
      event.preventDefault();
      await importAres(ico.value);
    });

    form.addEventListener('submit', async event => {
      event.preventDefault();
      showToast(query.value.trim() ? `Hledam: ${query.value.trim()}` : 'Zobrazuji vsechny firmy.');
      await search(query.value);
    });

    showAll.addEventListener('click', async () => {
      query.value = '';
      watchlistOnly = false;
      showWatchlist.classList.remove('active');
      showToast('Zobrazuji vsechny firmy.');
      await search('');
    });

    showWatchlist.addEventListener('click', () => {
      watchlistOnly = !watchlistOnly;
      showWatchlist.classList.toggle('active', watchlistOnly);
      showToast(watchlistOnly ? 'Zobrazuji watchlist.' : 'Zobrazuji vsechny vysledky.');
      renderCompanies(currentCompanies);
    });

    auditFilter.addEventListener('change', renderAuditLog);

    refreshAudit.addEventListener('click', async () => {
      await loadBackendAudit();
    });

    archiveAudit.addEventListener('click', () => {
      if (!auditEntries.length) {
        showToast('Audit log nema zadne aktivni zaznamy.');
        return;
      }
      auditEntries.forEach(entry => archivedAuditIds.add(entry.id));
      archivedAuditEntries = [...auditEntries, ...archivedAuditEntries];
      auditEntries = [];
      saveArchivedAuditState();
      auditArchiveVisible = true;
      archiveBox.hidden = false;
      showArchive.textContent = 'Skrýt archiv';
      showToast('Aktivni audit log byl presunut do archivu.');
      renderAuditLog();
    });

    showArchive.addEventListener('click', () => {
      auditArchiveVisible = !auditArchiveVisible;
      archiveBox.hidden = !auditArchiveVisible;
      showArchive.textContent = auditArchiveVisible ? 'Skrýt archiv' : 'Zobrazit archiv';
    });

    printAudit.addEventListener('click', () => {
      const archiveWasHidden = archiveBox.hidden;
      archiveBox.hidden = false;
      document.body.classList.add('print-audit');
      window.print();
      window.setTimeout(() => {
        document.body.classList.remove('print-audit');
        archiveBox.hidden = archiveWasHidden;
      }, 300);
    });

    // Import API actions
    async function importAres(value, refresh = true) {
      const normalized = String(value || '').replace(/\D/g, '');
      if (!normalized) {
        showToast('Zadej platne ICO.');
        addActivity('Neplatny vstup', 'ARES import byl zastaven, protoze ICO nebylo vyplneno.', 'warning');
        return;
      }

      showToast(`Importuji ICO ${normalized} z ARES...`);
      const response = await fetch(`/api/import/ares/${encodeURIComponent(normalized)}`, { method: 'POST' });
      if (!response.ok) {
        showToast(`ARES import selhal pro ICO ${normalized}.`);
        addActivity('ARES chyba', `Import ICO ${normalized} selhal.`, 'critical');
        await loadBackendAudit(false);
        return;
      }
      const company = await response.json();
      showToast(`Nahrano: ${company.name}`);
      addActivity('Import z ARES', `ICO ${normalized} ulozeno jako ${company.name}.`, 'info');
      await loadBackendAudit(false);
      if (refresh) {
        query.value = '';
        watchlistOnly = false;
        showWatchlist.classList.remove('active');
        await search('');
      }
    }

    async function importPayload(url, body, contentType, successMessage) {
      if (!String(body || '').trim()) {
        showToast('Nejdrive vloz data pro import.');
        addActivity('Prazdny import', 'Import byl zastaven, protoze vstup neobsahoval data.', 'warning');
        return;
      }

      showToast('Importuji data...');
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': contentType },
        body
      });
      if (!response.ok) {
        showToast('Import selhal. Zkontroluj format vstupu.');
        addActivity('Chyba importu', 'Import selhal pri kontrole formatu vstupnich dat.', 'critical');
        await loadBackendAudit(false);
        return;
      }
      const result = await response.json();
      showToast(successMessage);
      addActivity('Import dat', `${result.imported || 0} zaznamu ulozeno do databaze.`, 'info');
      await loadBackendAudit(false);
      query.value = '';
      watchlistOnly = false;
      showWatchlist.classList.remove('active');
      await search('');
    }

    // Search and company rendering
    async function search(value) {
      let response;
      try {
        response = await fetch(`/api/companies/search?q=${encodeURIComponent(value || '')}`);
      } catch (error) {
        setServerStatus(false);
        showToast('Server je offline.');
        addActivity('Server offline', 'Vyhledavani se nepodarilo spustit, API neodpovedelo.', 'critical');
        return;
      }
      if (!response.ok) {
        setServerStatus(false);
        showToast('Vyhledavani selhalo.');
        addActivity('Chyba vyhledavani', 'API vratilo chybu pri hledani firemnich zaznamu.', 'critical');
        return;
      }
      setServerStatus(true);
      currentCompanies = await response.json();
      addActivity('Vyhledavani', `${currentCompanies.length} zaznamu odpovida dotazu "${value || 'vse'}".`, 'low');
      if (selectedCompanyId && !currentCompanies.some(company => company.id === selectedCompanyId)) {
        selectedCompanyId = null;
        renderDetail(null);
      }
      renderCompanies(currentCompanies);
    }

    function renderCompanies(companies) {
      const visibleCompanies = watchlistOnly ? companies.filter(c => c.watchlisted) : companies;
      document.getElementById('count').textContent = visibleCompanies.length;
      document.getElementById('ares-count').textContent = visibleCompanies.filter(c => c.dataSource === 'ARES').length;
      document.getElementById('change-count').textContent = visibleCompanies.reduce((sum, c) => sum + ((c.changes || []).length), 0);
      document.getElementById('watch-count').textContent = companies.filter(c => c.watchlisted).length;

      if (!visibleCompanies.length) {
        results.innerHTML = watchlistOnly
          ? '<div class="empty">Watchlist je prazdny. Oznac firmu tlacitkem Sledovat.</div>'
          : '<div class="empty">Zadne firmy nebyly nalezeny. Nacti data z ARES, JSON nebo CSV, pripadne uprav vyhledavani.</div>';
        return;
      }

      if (!selectedCompanyId || !visibleCompanies.some(company => company.id === selectedCompanyId)) {
        selectedCompanyId = visibleCompanies[0].id;
        renderDetail(visibleCompanies[0]);
      }

      results.innerHTML = visibleCompanies.map(company => `
        <article class="company ${company.id === selectedCompanyId ? 'selected' : ''}">
          <div class="company-head">
            <div>
              <h3>${escapeHtml(company.name)}</h3>
              <p>${escapeHtml(company.address || 'Adresa neni uvedena')}</p>
            </div>
            <div class="company-actions">
              <button class="secondary" type="button" data-detail-id="${company.id}">Detail</button>
              <button class="watch ${company.watchlisted ? 'active' : ''}" type="button" data-watch-id="${company.id}" data-watch-state="${company.watchlisted ? 'false' : 'true'}">
                ${company.watchlisted ? 'Sledovano' : 'Sledovat'}
              </button>
              <span class="badge ${company.watchlisted ? 'watchlisted' : ''}">${company.watchlisted ? 'WATCHLIST' : escapeHtml(company.dataSource || 'LOCAL')}</span>
            </div>
          </div>
          <div class="meta">
            <span><strong>ICO:</strong> ${escapeHtml(company.registrationNumber)}</span>
            <span><strong>Stat:</strong> ${escapeHtml(company.country || '-')}</span>
            <span><strong>Pravni forma:</strong> ${escapeHtml(company.legalForm || '-')}</span>
            <span><strong>Historie:</strong> ${(company.changes || []).length} udalosti</span>
            <span><strong>Lide:</strong> ${(company.people || []).length}</span>
            <span><strong>Watchlist:</strong> ${company.watchlisted ? 'ano' : 'ne'}</span>
          </div>
          <details class="history">
            <summary>Zobrazit napojene lidi</summary>
            <div class="history-list">
              ${renderPeople(company.people || [])}
            </div>
          </details>
          <details class="history">
            <summary>Zobrazit historii zmen</summary>
            <div class="history-list">
              ${renderHistory(company.changes || [])}
            </div>
          </details>
        </article>
      `).join('');
    }

    results.addEventListener('click', async event => {
      const detailButton = event.target.closest('button[data-detail-id]');
      if (detailButton) {
        selectedCompanyId = Number(detailButton.dataset.detailId);
        renderDetail(currentCompanies.find(company => company.id === selectedCompanyId));
        renderCompanies(currentCompanies);
        return;
      }

      const watchButton = event.target.closest('button[data-watch-id]');
      if (!watchButton) {
        return;
      }
      const id = watchButton.dataset.watchId;
      const watchlisted = watchButton.dataset.watchState === 'true';
      const response = await fetch(`/api/companies/${id}/watchlist`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ watchlisted })
      });
      if (!response.ok) {
        showToast('Watchlist se nepodarilo ulozit.');
        addActivity('Watchlist chyba', 'Zmenu watchlistu se nepodarilo ulozit.', 'warning');
        return;
      }
      const updated = await response.json();
      currentCompanies = currentCompanies.map(company => company.id === updated.id ? updated : company);
      if (selectedCompanyId === updated.id) {
        renderDetail(updated);
      }
      showToast(watchlisted ? 'Firma pridana na watchlist.' : 'Firma odebrana z watchlistu.');
      addActivity('Watchlist', `${updated.name} ma stav: ${updated.watchlisted ? 'sledovano' : 'nesledovano'}.`, 'warning');
      await loadBackendAudit(false);
      renderCompanies(currentCompanies);
    });

    detailPanel.addEventListener('submit', async event => {
      if (event.target.id !== 'person-form') {
        return;
      }
      event.preventDefault();
      const formData = new FormData(event.target);
      const fullName = String(formData.get('fullName') || '').trim();
      const role = String(formData.get('role') || '').trim();
      const editingPersonId = event.target.dataset.editPersonId;
      if (!fullName) {
        showToast('Vypln jmeno osoby.');
        return;
      }
      const companyId = event.target.dataset.companyId;
      const url = editingPersonId
        ? `/api/companies/${companyId}/people/${editingPersonId}`
        : `/api/companies/${companyId}/people`;
      const response = await fetch(url, {
        method: editingPersonId ? 'PATCH' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fullName, role })
      });
      if (!response.ok) {
        showToast(editingPersonId ? 'Vazbu osoby se nepodarilo upravit.' : 'Osobu se nepodarilo priradit.');
        addActivity('Vazba osoby', editingPersonId ? 'Uprava vazby osoby selhala.' : 'Prirazeni osoby k firme selhalo.', 'warning');
        return;
      }
      const updated = await response.json();
      currentCompanies = currentCompanies.map(company => company.id === updated.id ? updated : company);
      selectedCompanyId = updated.id;
      renderCompanies(currentCompanies);
      renderDetail(updated);
      showToast(editingPersonId ? 'Vazba osoby byla upravena.' : 'Osoba byla prirazena k firme.');
      addActivity('Vazba osoby', editingPersonId ? `${fullName} upraven u firmy ${updated.name}.` : `${fullName} prirazen k firme ${updated.name}.`, 'warning');
      await loadBackendAudit(false);
    });

    detailPanel.addEventListener('click', async event => {
      const editButton = event.target.closest('button[data-edit-person-id]');
      if (editButton) {
        const form = document.getElementById('person-form');
        form.dataset.editPersonId = editButton.dataset.editPersonId;
        form.querySelector('input[name="fullName"]').value = editButton.dataset.fullName || '';
        form.querySelector('input[name="fullName"]').readOnly = true;
        form.querySelector('input[name="role"]').value = editButton.dataset.role || '';
        form.querySelector('button[type="submit"]').textContent = 'Ulozit';
        document.getElementById('person-cancel-edit').hidden = false;
        showToast('Uprav roli osoby a uloz zmenu.');
        return;
      }

      const cancelButton = event.target.closest('#person-cancel-edit');
      if (cancelButton) {
        resetPersonForm();
        showToast('Uprava osoby zrusena.');
        return;
      }

      const deleteButton = event.target.closest('button[data-delete-person-id]');
      if (!deleteButton) {
        return;
      }
      const companyId = deleteButton.dataset.companyId;
      const personId = deleteButton.dataset.deletePersonId;
      const response = await fetch(`/api/companies/${companyId}/people/${personId}`, { method: 'DELETE' });
      if (!response.ok) {
        showToast('Vazbu osoby se nepodarilo smazat.');
        addActivity('Vazba osoby', 'Smazani vazby osoby selhalo.', 'warning');
        return;
      }
      const updated = await response.json();
      currentCompanies = currentCompanies.map(company => company.id === updated.id ? updated : company);
      selectedCompanyId = updated.id;
      renderCompanies(currentCompanies);
      renderDetail(updated);
      showToast('Vazba osoby byla smazana.');
      addActivity('Vazba osoby', `Osoba odebrana od firmy ${updated.name}.`, 'warning');
      await loadBackendAudit(false);
    });

    function renderDetail(company) {
      if (!company) {
        detailPanel.innerHTML = `
          <h3>Detail firmy</h3>
          <p>Vyber firmu ve vysledcich. Tady se ukaze sjednoceny zaznam, napojene osoby, historie zmen a stav watchlistu.</p>
        `;
        return;
      }

      detailPanel.innerHTML = `
        <h3>${escapeHtml(company.name)}</h3>
        <p>${escapeHtml(company.address || 'Adresa neni uvedena')}</p>
        <div class="detail-section">
          <h4>Sjednoceny zaznam</h4>
          ${renderDetailRow('ICO', company.registrationNumber)}
          ${renderDetailRow('Stat', company.country || '-')}
          ${renderDetailRow('Pravni forma', company.legalForm || '-')}
          ${renderDetailRow('Zdroj', company.dataSource || 'LOCAL')}
          ${renderDetailRow('Watchlist', company.watchlisted ? 'ano' : 'ne')}
        </div>
        <div class="detail-section">
          <h4>Napojene osoby</h4>
          ${renderPeople(company.people || [], true, company.id)}
          <form class="person-form" id="person-form" data-company-id="${company.id}">
            <input name="fullName" autocomplete="off" placeholder="Jmeno osoby" aria-label="Jmeno osoby">
            <input name="role" autocomplete="off" placeholder="Role ve firme" aria-label="Role ve firme">
            <button type="submit">Priradit</button>
            <button class="secondary" type="button" id="person-cancel-edit" hidden>Zrusit</button>
          </form>
        </div>
        <div class="detail-section">
          <h4>Historie zmen</h4>
          ${renderHistory(company.changes || [])}
        </div>
      `;
    }

    function renderDetailRow(label, value) {
      return `
        <div class="detail-row">
          <strong>${escapeHtml(label)}</strong>
          <span>${escapeHtml(value)}</span>
        </div>
      `;
    }

    function resetPersonForm() {
      const form = document.getElementById('person-form');
      if (!form) {
        return;
      }
      delete form.dataset.editPersonId;
      form.reset();
      form.querySelector('input[name="fullName"]').readOnly = false;
      form.querySelector('button[type="submit"]').textContent = 'Priradit';
      document.getElementById('person-cancel-edit').hidden = true;
    }

    function renderPeople(people, editable = false, companyId = null) {
      if (!people.length) {
        return '<div class="history-item">U teto firmy zatim nejsou ulozene vazby na osoby.</div>';
      }

      return people.map(person => `
        <div class="history-item">
          <strong>${escapeHtml(person.fullName || 'Osoba')}</strong>
          <span>${escapeHtml(person.role || 'role neuvedena')}</span>
          ${editable ? `
            <div class="person-tools">
              <button class="secondary" type="button" data-edit-person-id="${person.personId}" data-full-name="${escapeHtml(person.fullName || '')}" data-role="${escapeHtml(person.role || '')}">Upravit</button>
              <button class="danger" type="button" data-delete-person-id="${person.personId}" data-company-id="${companyId}">Smazat</button>
            </div>
          ` : ''}
        </div>
      `).join('');
    }

    function renderHistory(changes) {
      if (!changes.length) {
        return '<div class="history-item">Historie zatim neobsahuje zadne udalosti.</div>';
      }

      return changes.map(change => `
        <div class="history-item">
          <strong>${escapeHtml(change.type || 'Zmena')}</strong>
          <span>${escapeHtml(change.description || '')}</span>
          <span>${formatDate(change.createdAt)}</span>
        </div>
      `).join('');
    }

    // Generic UI helpers
    function formatDate(value) {
      if (!value) {
        return '';
      }
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return escapeHtml(value);
      }
      return date.toLocaleString('cs-CZ');
    }

    function showToast(message) {
      toast.textContent = message;
      toast.classList.add('visible');
    }

    function setServerStatus(online) {
      serverStatus.classList.toggle('online', online);
      serverStatus.classList.toggle('offline', !online);
      statusTitle.textContent = online ? 'Server bezi' : 'Server offline';
    }

    function setSidebarCollapsed(collapsed) {
      shell.classList.toggle('sidebar-collapsed', collapsed);
      sidebarToggle.textContent = collapsed ? '›' : '‹';
      sidebarToggle.setAttribute('aria-label', collapsed ? 'Rozbalit menu' : 'Zasunout menu');
      sidebarToggle.setAttribute('title', collapsed ? 'Rozbalit menu' : 'Zasunout menu');
      localStorage.setItem('sidebarCollapsed', String(collapsed));
    }

    function showCurrentPage() {
      const allowedPages = pages.map(page => page.id);
      const requestedPage = window.location.hash.replace('#', '') || 'system';
      const activePage = allowedPages.includes(requestedPage) ? requestedPage : 'system';

      pages.forEach(page => {
        page.classList.toggle('active', page.id === activePage);
      });
      pageLinks.forEach(link => {
        link.classList.toggle('active', link.getAttribute('href') === `#${activePage}`);
      });
    }

    function loadStoredArray(key) {
      try {
        const value = JSON.parse(localStorage.getItem(key) || '[]');
        return Array.isArray(value) ? value : [];
      } catch (error) {
        return [];
      }
    }

    function storeArray(key, value) {
      try {
        localStorage.setItem(key, JSON.stringify(value));
      } catch (error) {
        // Storage can be unavailable in private or restricted browser contexts.
      }
    }

    // Audit log state and rendering
    function addActivity(title, message, level = 'info') {
      const time = new Date().toLocaleTimeString('cs-CZ', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
      auditEntries.unshift({ id: createLocalAuditId(), title, message, level, time });
      renderAuditLog();
    }

    async function loadBackendAudit(showMessage = true) {
      try {
        const response = await fetch('/api/audit?limit=100');
        if (!response.ok) {
          throw new Error('Audit endpoint failed');
        }
        const events = await response.json();
        auditEntries = events.map(event => ({
          id: createBackendAuditId(event),
          title: event.type || 'AUDIT',
          message: `${event.companyName || 'Firma'} (${event.registrationNumber || '-'}) - ${event.description || ''}`,
          level: mapSeverity(event.severity),
          time: formatDate(event.createdAt) || 'bez data'
        })).filter(entry => !archivedAuditIds.has(entry.id));
        renderAuditLog();
        if (showMessage) {
          showToast('Audit log nacten z backendu.');
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
      localAuditCounter += 1;
      return `local:${Date.now()}:${localAuditCounter}`;
    }

    function saveArchivedAuditState() {
      storeArray(ARCHIVED_AUDIT_IDS_KEY, Array.from(archivedAuditIds));
      storeArray(ARCHIVED_AUDIT_ENTRIES_KEY, archivedAuditEntries);
    }

    function renderAuditLog() {
      const filter = auditFilter.value;
      const visibleEntries = auditEntries.filter(entry => filter === 'all' || entry.level === filter);
      const recentEntries = auditEntries.slice(0, 2);

      activityLog.innerHTML = recentEntries.length
        ? recentEntries.map(entry => renderPreviewEntry(entry)).join('')
        : '<div class="activity-item"><strong>Audit zatim prazdny</strong><span>Aktivni log je prazdny.</span></div>';

      auditList.innerHTML = visibleEntries.length
        ? visibleEntries.map(entry => renderAuditEntry(entry)).join('')
        : '<div class="empty">Pro zvoleny filtr nejsou zadne aktivni udalosti.</div>';

      archiveList.innerHTML = archivedAuditEntries.length
        ? archivedAuditEntries.map(entry => renderAuditEntry(entry)).join('')
        : '<div class="empty">Archiv je zatim prazdny.</div>';

      document.getElementById('audit-total').textContent = auditEntries.length;
      document.getElementById('audit-critical').textContent = auditEntries.filter(entry => entry.level === 'critical').length;
      document.getElementById('audit-warning').textContent = auditEntries.filter(entry => entry.level === 'warning').length;
      document.getElementById('audit-archived').textContent = archivedAuditEntries.length;
    }

    function renderPreviewEntry(entry) {
      return `
        <div class="activity-item">
          <strong>${escapeHtml(entry.title)}</strong>
          <span>${escapeHtml(levelLabel(entry.level))}</span>
        </div>
      `;
    }

    function renderAuditEntry(entry) {
      return `
        <div class="audit-entry ${escapeHtml(entry.level)}">
          <div>
            <strong>${escapeHtml(entry.title)}</strong>
            <span class="audit-time">${escapeHtml(entry.time)}</span>
            <small>${escapeHtml(levelLabel(entry.level))}</small>
          </div>
          <span>${escapeHtml(entry.message)}</span>
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

    function escapeHtml(value) {
      return String(value || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
    }

    showCurrentPage();
    renderAuditLog();
    loadBackendAudit(false);
    search('');
