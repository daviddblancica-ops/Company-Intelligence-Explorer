import { api } from './api.js';
import { byId, escapeHtml, formatDate, setServerStatus, showToast, updateText } from './ui.js';

export function initCompanies({ audit, navigation, onChanged = async () => {}, onPeopleChanged = async () => {} }) {
  const form = byId('search-form');
  const query = byId('query');
  const results = byId('results');
  const detailPanel = byId('detail-panel');
  const showAll = byId('show-all');
  const showWatchlist = byId('show-watchlist');
  let companies = [];
  let selectedId = null;
  let watchlistOnly = false;

  function init() {
    form.addEventListener('submit', event => {
      event.preventDefault();
      search(query.value);
    });
    showAll.addEventListener('click', () => {
      query.value = '';
      watchlistOnly = false;
      showWatchlist.classList.remove('active');
      showWatchlist.setAttribute('aria-pressed', 'false');
      search('');
    });
    showWatchlist.addEventListener('click', () => {
      watchlistOnly = !watchlistOnly;
      showWatchlist.classList.toggle('active', watchlistOnly);
      showWatchlist.setAttribute('aria-pressed', String(watchlistOnly));
      showToast(watchlistOnly ? 'Zobrazuji watchlist.' : 'Zobrazuji vsechny vysledky.');
      renderList();
    });
    results.addEventListener('click', handleListAction);
    detailPanel.addEventListener('submit', handlePersonSubmit);
    detailPanel.addEventListener('click', handleDetailAction);
  }

  async function search(value = '') {
    const term = String(value || '').trim();
    try {
      companies = await api.get(`/api/companies/search?q=${encodeURIComponent(term)}`);
      setServerStatus(true);
      audit.addActivity('Vyhledavani', `${companies.length} zaznamu odpovida dotazu "${term || 'vse'}".`, 'low');
      if (selectedId && !companies.some(company => company.id === selectedId)) {
        selectedId = null;
      }
      renderList();
    } catch (error) {
      setServerStatus(false);
      showToast(error.status === 0 ? 'Server je offline.' : 'Vyhledavani selhalo.');
      audit.addActivity('Chyba vyhledavani', 'API vratilo chybu pri hledani firemnich zaznamu.', 'critical');
    }
  }

  async function openByRegistration(registrationNumber) {
    navigation.show('search');
    query.value = registrationNumber || '';
    watchlistOnly = false;
    showWatchlist.classList.remove('active');
    showWatchlist.setAttribute('aria-pressed', 'false');
    await search(query.value);
  }

  async function handleListAction(event) {
    const detailButton = event.target.closest('button[data-detail-id]');
    if (detailButton) {
      selectedId = Number(detailButton.dataset.detailId);
      renderList();
      return;
    }

    const watchButton = event.target.closest('button[data-watch-id]');
    if (!watchButton) return;
    const id = Number(watchButton.dataset.watchId);
    const watchlisted = watchButton.dataset.watchState === 'true';
    try {
      const updated = await api.patch(`/api/companies/${id}/watchlist`, { watchlisted });
      replaceCompany(updated);
      showToast(watchlisted ? 'Firma pridana na watchlist.' : 'Firma odebrana z watchlistu.');
      audit.addActivity('Watchlist', `${updated.name} ma stav: ${updated.watchlisted ? 'sledovano' : 'nesledovano'}.`, 'warning');
      renderList();
      await onChanged();
    } catch (error) {
      showToast('Watchlist se nepodarilo ulozit.');
      audit.addActivity('Watchlist chyba', 'Zmenu watchlistu se nepodarilo ulozit.', 'warning');
    }
  }

  async function handlePersonSubmit(event) {
    if (event.target.id !== 'person-form') return;
    event.preventDefault();
    const data = new FormData(event.target);
    const fullName = String(data.get('fullName') || '').trim();
    const role = String(data.get('role') || '').trim();
    const companyId = event.target.dataset.companyId;
    const personId = event.target.dataset.editPersonId;
    if (!fullName) {
      showToast('Vypln jmeno osoby.');
      return;
    }
    try {
      const updated = personId
        ? await api.patch(`/api/companies/${companyId}/people/${personId}`, { fullName, role })
        : await api.post(`/api/companies/${companyId}/people`, { fullName, role });
      replaceCompany(updated);
      selectedId = updated.id;
      showToast(personId ? 'Role osoby byla upravena.' : 'Osoba byla prirazena k firme.');
      audit.addActivity('Vazba osoby', personId
        ? `${fullName}: role upravena u firmy ${updated.name}.`
        : `${fullName} prirazen k firme ${updated.name}.`, 'warning');
      renderList();
      await Promise.allSettled([onChanged(), onPeopleChanged()]);
    } catch (error) {
      showToast(personId ? 'Roli osoby se nepodarilo upravit.' : 'Osobu se nepodarilo priradit.');
      audit.addActivity('Vazba osoby', 'Ulozeni vazby osoby selhalo.', 'warning');
    }
  }

  async function handleDetailAction(event) {
    const editButton = event.target.closest('button[data-edit-person-id]');
    if (editButton) {
      const personForm = byId('person-form');
      personForm.dataset.editPersonId = editButton.dataset.editPersonId;
      personForm.querySelector('[name="fullName"]').value = editButton.dataset.fullName || '';
      personForm.querySelector('[name="fullName"]').readOnly = true;
      personForm.querySelector('[name="role"]').value = editButton.dataset.role || '';
      personForm.querySelector('button[type="submit"]').textContent = 'Ulozit roli';
      byId('person-cancel-edit').hidden = false;
      personForm.querySelector('[name="role"]').focus();
      return;
    }
    if (event.target.closest('#person-cancel-edit')) {
      resetPersonForm();
      return;
    }
    const deleteButton = event.target.closest('button[data-delete-person-id]');
    if (!deleteButton) return;
    const companyId = deleteButton.dataset.companyId;
    const personId = deleteButton.dataset.deletePersonId;
    try {
      const updated = await api.delete(`/api/companies/${companyId}/people/${personId}`);
      replaceCompany(updated);
      showToast('Vazba osoby byla odstranena.');
      audit.addActivity('Vazba osoby', `Osoba odebrana od firmy ${updated.name}.`, 'warning');
      renderList();
      await Promise.allSettled([onChanged(), onPeopleChanged()]);
    } catch (error) {
      showToast('Vazbu osoby se nepodarilo odstranit.');
    }
  }

  function replaceCompany(updated) {
    companies = companies.map(company => company.id === updated.id ? updated : company);
  }

  function visibleCompanies() {
    return watchlistOnly ? companies.filter(company => company.watchlisted) : companies;
  }

  function renderList() {
    const visible = visibleCompanies();
    updateText('count', visible.length);
    updateText('ares-count', visible.filter(company => company.dataSource === 'ARES').length);
    updateText('change-count', visible.reduce((sum, company) => sum + (company.changes || []).length, 0));
    updateText('watch-count', companies.filter(company => company.watchlisted).length);

    if (!visible.length) {
      results.innerHTML = `<div class="empty">${watchlistOnly
        ? 'Watchlist je prazdny. Oznac firmu tlacitkem Sledovat.'
        : 'Zadne firmy nebyly nalezeny. Nacti data nebo uprav vyhledavani.'}</div>`;
      renderDetail(null);
      return;
    }
    if (!selectedId || !visible.some(company => company.id === selectedId)) {
      selectedId = visible[0].id;
    }
    results.innerHTML = visible.map(company => `
      <article class="company compact-company ${company.id === selectedId ? 'selected' : ''}">
        <div class="company-head">
          <div><h3>${escapeHtml(company.name)}</h3><p>ICO ${escapeHtml(company.registrationNumber)} · ${escapeHtml(company.address || 'adresa neuvedena')}</p></div>
          <div class="company-actions">
            <button class="secondary" type="button" data-detail-id="${company.id}">Detail</button>
            <button class="watch ${company.watchlisted ? 'active' : ''}" type="button"
              data-watch-id="${company.id}" data-watch-state="${company.watchlisted ? 'false' : 'true'}"
              aria-pressed="${company.watchlisted}">${company.watchlisted ? 'Sledovano' : 'Sledovat'}</button>
          </div>
        </div>
        <div class="meta">
          <span>${escapeHtml(company.legalForm || '-')}</span>
          <span>${(company.people || []).length} osob</span>
          <span>${(company.changes || []).length} zmen</span>
          <span class="badge ${company.watchlisted ? 'watchlisted' : ''}">${company.watchlisted ? 'WATCHLIST' : escapeHtml(company.dataSource || 'LOCAL')}</span>
        </div>
      </article>`).join('');
    renderDetail(visible.find(company => company.id === selectedId));
  }

  function renderDetail(company) {
    if (!company) {
      detailPanel.innerHTML = '<div class="empty detail-empty"><h3>Detail firmy</h3><p>Vyber firmu ve vysledcich.</p></div>';
      return;
    }
    detailPanel.innerHTML = `
      <div class="detail-heading"><div><h3>${escapeHtml(company.name)}</h3><p>${escapeHtml(company.address || 'Adresa neni uvedena')}</p></div>
        <span class="badge ${company.watchlisted ? 'watchlisted' : ''}">${company.watchlisted ? 'WATCHLIST' : escapeHtml(company.dataSource || 'LOCAL')}</span></div>
      <div class="detail-section"><h4>Zakladni udaje</h4>
        ${detailRow('ICO', company.registrationNumber)}${detailRow('Stat', company.country || '-')}${detailRow('Pravni forma', company.legalForm || '-')}
      </div>
      <div class="detail-section"><div class="section-heading"><h4>Osoby a role</h4><span>${(company.people || []).length}</span></div>
        <div class="relationship-list">${renderCompanyPeople(company.people || [], company.id)}</div>
        <form class="person-form" id="person-form" data-company-id="${company.id}">
          <input name="fullName" autocomplete="off" placeholder="Jmeno osoby" aria-label="Jmeno osoby" required>
          <input name="role" autocomplete="off" placeholder="Role ve firme" aria-label="Role ve firme">
          <button type="submit">Priradit</button><button class="secondary" type="button" id="person-cancel-edit" hidden>Zrusit</button>
        </form>
      </div>
      <details class="history"><summary>Historie zmen (${(company.changes || []).length})</summary>
        <div class="history-list">${renderHistory(company.changes || [])}</div></details>`;
  }

  function renderCompanyPeople(people, companyId) {
    if (!people.length) {
      return '<div class="history-item">U teto firmy zatim nejsou ulozene vazby na osoby.</div>';
    }
    return people.map(person => `
      <div class="relationship-item">
        <div><strong>${escapeHtml(person.fullName || 'Osoba')}</strong><span>${escapeHtml(person.role || 'role neuvedena')}</span></div>
        <div class="relationship-actions">
          <button class="secondary icon-action" type="button" data-edit-person-id="${person.personId}"
            data-full-name="${escapeHtml(person.fullName)}" data-role="${escapeHtml(person.role || '')}" title="Upravit roli" aria-label="Upravit roli">Upravit</button>
          <button class="secondary icon-action danger" type="button" data-delete-person-id="${person.personId}"
            data-company-id="${companyId}" title="Odstranit vazbu" aria-label="Odstranit vazbu">Odebrat</button>
        </div>
      </div>`).join('');
  }

  function renderHistory(changes) {
    if (!changes.length) return '<div class="history-item">Historie zatim neobsahuje zadne udalosti.</div>';
    return changes.map(change => `<div class="history-item"><strong>${escapeHtml(change.type || 'Zmena')}</strong>
      <span>${escapeHtml(change.description || '')}</span><span>${formatDate(change.createdAt)}</span></div>`).join('');
  }

  function detailRow(label, value) {
    return `<div class="detail-row"><strong>${escapeHtml(label)}</strong><span>${escapeHtml(value)}</span></div>`;
  }

  function resetPersonForm() {
    const personForm = document.getElementById('person-form');
    if (!personForm) return;
    delete personForm.dataset.editPersonId;
    personForm.reset();
    personForm.querySelector('[name="fullName"]').readOnly = false;
    personForm.querySelector('button[type="submit"]').textContent = 'Priradit';
    byId('person-cancel-edit').hidden = true;
  }

  return { init, search, openByRegistration };
}
