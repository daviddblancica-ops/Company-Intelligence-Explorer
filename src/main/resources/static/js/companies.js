export function initCompanies({ dom, state, ui, audit }) {
  dom.form.addEventListener('submit', async event => {
    event.preventDefault();
    ui.showToast(dom.query.value.trim() ? `Hledam: ${dom.query.value.trim()}` : 'Zobrazuji vsechny firmy.');
    await search(dom.query.value);
  });

  dom.showAll.addEventListener('click', async () => {
    dom.query.value = '';
    state.watchlistOnly = false;
    dom.showWatchlist.classList.remove('active');
    ui.showToast('Zobrazuji vsechny firmy.');
    await search('');
  });

  dom.showWatchlist.addEventListener('click', () => {
    state.watchlistOnly = !state.watchlistOnly;
    dom.showWatchlist.classList.toggle('active', state.watchlistOnly);
    ui.showToast(state.watchlistOnly ? 'Zobrazuji watchlist.' : 'Zobrazuji vsechny vysledky.');
    renderCompanies(state.currentCompanies);
  });

  dom.results.addEventListener('click', async event => {
    const detailButton = event.target.closest('button[data-detail-id]');
    if (detailButton) {
      state.selectedCompanyId = Number(detailButton.dataset.detailId);
      renderDetail(state.currentCompanies.find(company => company.id === state.selectedCompanyId));
      renderCompanies(state.currentCompanies);
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
      ui.showToast('Watchlist se nepodarilo ulozit.');
      audit.addActivity('Watchlist chyba', 'Zmenu watchlistu se nepodarilo ulozit.', 'warning');
      return;
    }
    const updated = await response.json();
    state.currentCompanies = state.currentCompanies.map(company => company.id === updated.id ? updated : company);
    if (state.selectedCompanyId === updated.id) {
      renderDetail(updated);
    }
    ui.showToast(watchlisted ? 'Firma pridana na watchlist.' : 'Firma odebrana z watchlistu.');
    audit.addActivity('Watchlist', `${updated.name} ma stav: ${updated.watchlisted ? 'sledovano' : 'nesledovano'}.`, 'warning');
    await audit.loadBackendAudit(false);
    renderCompanies(state.currentCompanies);
  });

  dom.detailPanel.addEventListener('submit', async event => {
    if (event.target.id !== 'person-form') {
      return;
    }
    event.preventDefault();
    const formData = new FormData(event.target);
    const fullName = String(formData.get('fullName') || '').trim();
    const role = String(formData.get('role') || '').trim();
    const editingPersonId = event.target.dataset.editPersonId;
    if (!fullName) {
      ui.showToast('Vypln jmeno osoby.');
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
      ui.showToast(editingPersonId ? 'Vazbu osoby se nepodarilo upravit.' : 'Osobu se nepodarilo priradit.');
      audit.addActivity('Vazba osoby', editingPersonId ? 'Uprava vazby osoby selhala.' : 'Prirazeni osoby k firme selhalo.', 'warning');
      return;
    }
    const updated = await response.json();
    state.currentCompanies = state.currentCompanies.map(company => company.id === updated.id ? updated : company);
    state.selectedCompanyId = updated.id;
    renderCompanies(state.currentCompanies);
    renderDetail(updated);
    ui.showToast(editingPersonId ? 'Vazba osoby byla upravena.' : 'Osoba byla prirazena k firme.');
    audit.addActivity('Vazba osoby', editingPersonId ? `${fullName} upraven u firmy ${updated.name}.` : `${fullName} prirazen k firme ${updated.name}.`, 'warning');
    await audit.loadBackendAudit(false);
  });

  dom.detailPanel.addEventListener('click', async event => {
    const editButton = event.target.closest('button[data-edit-person-id]');
    if (editButton) {
      const form = document.getElementById('person-form');
      form.dataset.editPersonId = editButton.dataset.editPersonId;
      form.querySelector('input[name="fullName"]').value = editButton.dataset.fullName || '';
      form.querySelector('input[name="fullName"]').readOnly = true;
      form.querySelector('input[name="role"]').value = editButton.dataset.role || '';
      form.querySelector('button[type="submit"]').textContent = 'Ulozit';
      document.getElementById('person-cancel-edit').hidden = false;
      ui.showToast('Uprav roli osoby a uloz zmenu.');
      return;
    }

    const cancelButton = event.target.closest('#person-cancel-edit');
    if (cancelButton) {
      resetPersonForm();
      ui.showToast('Uprava osoby zrusena.');
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
      ui.showToast('Vazbu osoby se nepodarilo smazat.');
      audit.addActivity('Vazba osoby', 'Smazani vazby osoby selhalo.', 'warning');
      return;
    }
    const updated = await response.json();
    state.currentCompanies = state.currentCompanies.map(company => company.id === updated.id ? updated : company);
    state.selectedCompanyId = updated.id;
    renderCompanies(state.currentCompanies);
    renderDetail(updated);
    ui.showToast('Vazba osoby byla smazana.');
    audit.addActivity('Vazba osoby', `Osoba odebrana od firmy ${updated.name}.`, 'warning');
    await audit.loadBackendAudit(false);
  });

  async function search(value) {
    let response;
    try {
      response = await fetch(`/api/companies/search?q=${encodeURIComponent(value || '')}`);
    } catch (error) {
      ui.setServerStatus(false);
      ui.showToast('Server je offline.');
      audit.addActivity('Server offline', 'Vyhledavani se nepodarilo spustit, API neodpovedelo.', 'critical');
      return;
    }
    if (!response.ok) {
      ui.setServerStatus(false);
      ui.showToast('Vyhledavani selhalo.');
      audit.addActivity('Chyba vyhledavani', 'API vratilo chybu pri hledani firemnich zaznamu.', 'critical');
      return;
    }
    ui.setServerStatus(true);
    state.currentCompanies = await response.json();
    audit.addActivity('Vyhledavani', `${state.currentCompanies.length} zaznamu odpovida dotazu "${value || 'vse'}".`, 'low');
    if (state.selectedCompanyId && !state.currentCompanies.some(company => company.id === state.selectedCompanyId)) {
      state.selectedCompanyId = null;
      renderDetail(null);
    }
    renderCompanies(state.currentCompanies);
  }

  function renderCompanies(companies) {
    const visibleCompanies = state.watchlistOnly ? companies.filter(c => c.watchlisted) : companies;
    dom.count.textContent = visibleCompanies.length;
    dom.aresCount.textContent = visibleCompanies.filter(c => c.dataSource === 'ARES').length;
    dom.changeCount.textContent = visibleCompanies.reduce((sum, c) => sum + ((c.changes || []).length), 0);
    dom.watchCount.textContent = companies.filter(c => c.watchlisted).length;

    if (!visibleCompanies.length) {
      dom.results.innerHTML = state.watchlistOnly
        ? '<div class="empty">Watchlist je prazdny. Oznac firmu tlacitkem Sledovat.</div>'
        : '<div class="empty">Zadne firmy nebyly nalezeny. Nacti data z ARES, JSON nebo CSV, pripadne uprav vyhledavani.</div>';
      return;
    }

    if (!state.selectedCompanyId || !visibleCompanies.some(company => company.id === state.selectedCompanyId)) {
      state.selectedCompanyId = visibleCompanies[0].id;
      renderDetail(visibleCompanies[0]);
    }

    dom.results.innerHTML = visibleCompanies.map(company => `
      <article class="company ${company.id === state.selectedCompanyId ? 'selected' : ''}">
        <div class="company-head">
          <div>
            <h3>${ui.escapeHtml(company.name)}</h3>
            <p>${ui.escapeHtml(company.address || 'Adresa neni uvedena')}</p>
          </div>
          <div class="company-actions">
            <button class="secondary" type="button" data-detail-id="${company.id}">Detail</button>
            <button class="watch ${company.watchlisted ? 'active' : ''}" type="button" data-watch-id="${company.id}" data-watch-state="${company.watchlisted ? 'false' : 'true'}">
              ${company.watchlisted ? 'Sledovano' : 'Sledovat'}
            </button>
            <span class="badge ${company.watchlisted ? 'watchlisted' : ''}">${company.watchlisted ? 'WATCHLIST' : ui.escapeHtml(company.dataSource || 'LOCAL')}</span>
          </div>
        </div>
        <div class="meta">
          <span><strong>ICO:</strong> ${ui.escapeHtml(company.registrationNumber)}</span>
          <span><strong>Stat:</strong> ${ui.escapeHtml(company.country || '-')}</span>
          <span><strong>Pravni forma:</strong> ${ui.escapeHtml(company.legalForm || '-')}</span>
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

  function renderDetail(company) {
    if (!company) {
      dom.detailPanel.innerHTML = `
        <h3>Detail firmy</h3>
        <p>Vyber firmu ve vysledcich. Tady se ukaze sjednoceny zaznam, napojene osoby, historie zmen a stav watchlistu.</p>
      `;
      return;
    }

    dom.detailPanel.innerHTML = `
      <h3>${ui.escapeHtml(company.name)}</h3>
      <p>${ui.escapeHtml(company.address || 'Adresa neni uvedena')}</p>
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
        <strong>${ui.escapeHtml(label)}</strong>
        <span>${ui.escapeHtml(value)}</span>
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
        <strong>${ui.escapeHtml(person.fullName || 'Osoba')}</strong>
        <span>${ui.escapeHtml(person.role || 'role neuvedena')}</span>
        ${editable ? `
          <div class="person-tools">
            <button class="secondary" type="button" data-edit-person-id="${person.personId}" data-full-name="${ui.escapeHtml(person.fullName || '')}" data-role="${ui.escapeHtml(person.role || '')}">Upravit</button>
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
        <strong>${ui.escapeHtml(change.type || 'Zmena')}</strong>
        <span>${ui.escapeHtml(change.description || '')}</span>
        <span>${ui.formatDate(change.createdAt)}</span>
      </div>
    `).join('');
  }

  return {
    search,
    renderCompanies
  };
}
