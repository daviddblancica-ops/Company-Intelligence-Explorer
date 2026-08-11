import { api } from './api.js';
import { byId, escapeHtml, formatDate, setServerStatus, showToast, updateText } from './ui.js';

export function initCompanies({ audit, navigation, onChanged = async () => {}, onPeopleChanged = async () => {} }) {
  const form = byId('search-form');
  const query = byId('query');
  const results = byId('results');
  const showAll = byId('show-all');
  const showWatchlist = byId('show-watchlist');
  const editDialog = byId('company-edit-dialog');
  const editForm = byId('company-edit-form');
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
      showToast(watchlistOnly ? 'Zobrazuji watchlist.' : 'Zobrazuji všechny výsledky.');
      if (selectedId && !visibleCompanies().some(company => company.id === selectedId)) selectedId = null;
      renderList();
    });
    results.addEventListener('click', handleResultAction);
    results.addEventListener('submit', handlePersonSubmit);
    editForm.addEventListener('submit', saveCompanyEdit);
    document.querySelectorAll('[data-close-dialog="company-edit-dialog"]').forEach(button => {
      button.addEventListener('click', () => editDialog.close());
    });
  }

  async function search(value = '') {
    const term = String(value || '').trim();
    try {
      companies = await api.get(`/api/companies/search?q=${encodeURIComponent(term)}`);
      setServerStatus(true);
      audit.addActivity('Vyhledávání', `${companies.length} záznamů odpovídá dotazu "${term || 'vše'}".`, 'low');
      if (selectedId && !companies.some(company => company.id === selectedId)) selectedId = null;
      renderList();
    } catch (error) {
      setServerStatus(false);
      showToast(error.status === 0 ? 'Server je offline.' : 'Vyhledávání selhalo.');
      audit.addActivity('Chyba vyhledávání', 'API vrátilo chybu při hledání firemních záznamů.', 'critical');
    }
  }

  async function openByRegistration(registrationNumber) {
    navigation.show('search');
    query.value = registrationNumber || '';
    watchlistOnly = false;
    showWatchlist.classList.remove('active');
    showWatchlist.setAttribute('aria-pressed', 'false');
    await search(query.value);
    const company = companies.find(item => item.registrationNumber === registrationNumber) || companies[0];
    selectedId = company ? company.id : null;
    renderList();
  }

  async function handleResultAction(event) {
    const detailButton = event.target.closest('button[data-detail-id]');
    if (detailButton) {
      const id = Number(detailButton.dataset.detailId);
      selectedId = selectedId === id ? null : id;
      renderList();
      return;
    }

    const watchButton = event.target.closest('button[data-watch-id]');
    if (watchButton) {
      await setWatchlist(watchButton);
      return;
    }

    const editCompanyButton = event.target.closest('button[data-edit-company-id]');
    if (editCompanyButton) {
      const company = companies.find(item => item.id === Number(editCompanyButton.dataset.editCompanyId));
      if (company) openEditDialog(company);
      return;
    }

    const deleteCompanyButton = event.target.closest('button[data-delete-company-id]');
    if (deleteCompanyButton) {
      await deleteCompany(Number(deleteCompanyButton.dataset.deleteCompanyId));
      return;
    }

    const editPersonButton = event.target.closest('button[data-edit-person-id]');
    if (editPersonButton) {
      const company = selectedCompany();
      const person = company?.people?.find(item => item.personId === Number(editPersonButton.dataset.editPersonId));
      if (person) startRoleEdit(person);
      return;
    }

    if (event.target.closest('#person-cancel-edit')) {
      resetPersonForm();
      return;
    }

    const deletePersonButton = event.target.closest('button[data-delete-person-id]');
    if (deletePersonButton) await removePersonRelationship(deletePersonButton);
  }

  async function setWatchlist(button) {
    const id = Number(button.dataset.watchId);
    const watchlisted = button.dataset.watchState === 'true';
    try {
      const updated = await api.patch(`/api/companies/${id}/watchlist`, { watchlisted });
      replaceCompany(updated);
      showToast(watchlisted ? 'Firma přidána na watchlist.' : 'Firma odebrána z watchlistu.');
      audit.addActivity('Watchlist', `${updated.name} má stav: ${updated.watchlisted ? 'sledováno' : 'nesledováno'}.`, 'warning');
      renderList();
      await onChanged();
    } catch (error) {
      showToast('Watchlist se nepodařilo uložit.');
      audit.addActivity('Chyba watchlistu', 'Změnu watchlistu se nepodařilo uložit.', 'warning');
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
      showToast('Vyplň jméno osoby.');
      return;
    }
    try {
      const updated = personId
        ? await api.patch(`/api/companies/${companyId}/people/${personId}`, { fullName, role })
        : await api.post(`/api/companies/${companyId}/people`, { fullName, role });
      replaceCompany(updated);
      selectedId = updated.id;
      showToast(personId ? 'Role osoby byla upravena.' : 'Osoba byla přiřazena k firmě.');
      audit.addActivity('Vazba osoby', personId
        ? `${fullName}: role upravena u firmy ${updated.name}.`
        : `${fullName} přiřazen k firmě ${updated.name}.`, 'warning');
      renderList();
      await Promise.allSettled([onChanged(), onPeopleChanged()]);
    } catch (error) {
      showToast(personId ? 'Roli osoby se nepodařilo upravit.' : 'Osobu se nepodařilo přiřadit.');
      audit.addActivity('Vazba osoby', 'Uložení vazby osoby selhalo.', 'warning');
    }
  }

  function startRoleEdit(person) {
    const personForm = byId('person-form');
    personForm.dataset.editPersonId = person.personId;
    personForm.querySelector('[name="fullName"]').value = person.fullName || '';
    personForm.querySelector('[name="fullName"]').readOnly = true;
    personForm.querySelector('[name="role"]').value = person.role || '';
    personForm.querySelector('button[type="submit"]').textContent = 'Uložit roli';
    byId('person-cancel-edit').hidden = false;
    personForm.querySelector('[name="role"]').focus();
  }

  async function removePersonRelationship(button) {
    const companyId = button.dataset.companyId;
    const personId = button.dataset.deletePersonId;
    if (!window.confirm('Odebrat tuto osobu od firmy? Samotný záznam osoby zůstane v registru.')) return;
    try {
      const updated = await api.delete(`/api/companies/${companyId}/people/${personId}`);
      replaceCompany(updated);
      showToast('Vazba osoby byla odstraněna.');
      audit.addActivity('Vazba osoby', `Osoba odebrána od firmy ${updated.name}.`, 'warning');
      renderList();
      await Promise.allSettled([onChanged(), onPeopleChanged()]);
    } catch (error) {
      showToast('Vazbu osoby se nepodařilo odstranit.');
    }
  }

  function openEditDialog(company) {
    editForm.elements.id.value = company.id;
    editForm.elements.name.value = company.name || '';
    editForm.elements.registrationNumber.value = company.registrationNumber || '';
    editForm.elements.country.value = company.country || '';
    editForm.elements.legalForm.value = company.legalForm || '';
    editForm.elements.address.value = company.address || '';
    editForm.elements.dataSource.value = company.dataSource || '';
    editDialog.showModal();
    editForm.elements.name.focus();
  }

  async function saveCompanyEdit(event) {
    event.preventDefault();
    const data = new FormData(editForm);
    const id = Number(data.get('id'));
    const payload = {
      name: String(data.get('name') || '').trim(),
      registrationNumber: String(data.get('registrationNumber') || '').trim(),
      country: String(data.get('country') || '').trim(),
      legalForm: String(data.get('legalForm') || '').trim(),
      address: String(data.get('address') || '').trim(),
      dataSource: String(data.get('dataSource') || '').trim()
    };
    try {
      const updated = await api.put(`/api/companies/${id}`, payload);
      replaceCompany(updated);
      selectedId = updated.id;
      editDialog.close();
      renderList();
      showToast('Firemní záznam byl upraven.');
      audit.addActivity('Úprava firmy', `${updated.name} byla upravena.`, 'warning');
      await onChanged();
    } catch (error) {
      showToast(error.message || 'Firmu se nepodařilo uložit.');
    }
  }

  async function deleteCompany(id) {
    const company = companies.find(item => item.id === id);
    if (!company || !window.confirm(`Opravdu smazat firmu ${company.name}? Vazby zmizí, audit zůstane zachován.`)) return;
    try {
      await api.delete(`/api/companies/${id}`);
      companies = companies.filter(item => item.id !== id);
      selectedId = null;
      renderList();
      showToast('Firma byla smazana z registru.');
      audit.addActivity('Smazání firmy', `${company.name} byla smazána z registru.`, 'critical');
      await Promise.allSettled([onChanged(), onPeopleChanged()]);
    } catch (error) {
      showToast(error.message || 'Firmu se nepodařilo smazat.');
    }
  }

  function replaceCompany(updated) {
    companies = companies.map(company => company.id === updated.id ? updated : company);
  }

  function selectedCompany() {
    return companies.find(company => company.id === selectedId);
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
        ? 'Watchlist je prázdný. Označ firmu tlačítkem Sledovat.'
        : 'Žádné firmy nebyly nalezeny. Načti data nebo uprav vyhledávání.'}</div>`;
      return;
    }
    results.innerHTML = visible.map(company => renderCompanyEntry(company)).join('');
  }

  function renderCompanyEntry(company) {
    const expanded = company.id === selectedId;
    return `<div class="registry-entry">
      <article class="company compact-company ${expanded ? 'selected expanded' : ''}">
        <div class="company-head">
          <div><h3>${escapeHtml(company.name)}</h3><p>IČO ${escapeHtml(company.registrationNumber)} · ${escapeHtml(company.address || 'adresa neuvedena')}</p></div>
          <div class="company-actions">
            <button class="secondary" type="button" data-detail-id="${company.id}" aria-expanded="${expanded}">${expanded ? 'Skrýt' : 'Detail'}</button>
            <button class="watch ${company.watchlisted ? 'active' : ''}" type="button"
              data-watch-id="${company.id}" data-watch-state="${company.watchlisted ? 'false' : 'true'}"
              aria-pressed="${company.watchlisted}">${company.watchlisted ? 'Sledováno' : 'Sledovat'}</button>
          </div>
        </div>
        <div class="meta">
          <span>${escapeHtml(company.legalForm || '-')}</span>
          <span>${(company.people || []).length} osob</span>
          <span>${(company.changes || []).length} změn</span>
          <span class="badge ${company.watchlisted ? 'watchlisted' : ''}">${company.watchlisted ? 'WATCHLIST' : escapeHtml(company.dataSource || 'LOCAL')}</span>
        </div>
      </article>
      ${expanded ? renderDetail(company) : ''}
    </div>`;
  }

  function renderDetail(company) {
    return `<section class="inline-detail" aria-label="Detail firmy ${escapeHtml(company.name)}">
      <div class="detail-heading"><div><h3>${escapeHtml(company.name)}</h3><p>${escapeHtml(company.address || 'Adresa není uvedena')}</p></div>
        <div class="record-actions"><button class="secondary" type="button" data-edit-company-id="${company.id}">Upravit firmu</button>
          <button class="danger" type="button" data-delete-company-id="${company.id}">Smazat firmu</button></div></div>
      <div class="detail-section"><h4>Základní údaje</h4>
        ${detailRow('IČO', company.registrationNumber)}${detailRow('Stát', company.country || '-')}${detailRow('Právní forma', company.legalForm || '-')}${detailRow('Zdroj', company.dataSource || '-')}
      </div>
      <div class="detail-section"><div class="section-heading"><h4>Osoby a role</h4><span>${(company.people || []).length}</span></div>
        <div class="relationship-list">${renderCompanyPeople(company.people || [], company.id)}</div>
        <form class="person-form" id="person-form" data-company-id="${company.id}">
          <input name="fullName" autocomplete="off" placeholder="Jméno osoby" aria-label="Jméno osoby" required>
          <input name="role" autocomplete="off" placeholder="Role ve firmě" aria-label="Role ve firmě">
          <button type="submit">Přiřadit</button><button class="secondary" type="button" id="person-cancel-edit" hidden>Zrušit</button>
        </form>
      </div>
      <details class="history"><summary>Historie změn (${(company.changes || []).length})</summary>
        <div class="history-list">${renderHistory(company.changes || [])}</div></details>
    </section>`;
  }

  function renderCompanyPeople(people, companyId) {
    if (!people.length) return '<div class="history-item">U této firmy zatím nejsou uložené vazby na osoby.</div>';
    return people.map(person => `
      <div class="relationship-item">
        <div><strong>${escapeHtml(person.fullName || 'Osoba')}</strong><span>${escapeHtml(person.role || 'role neuvedena')}</span></div>
        <div class="relationship-actions">
          <button class="secondary icon-action" type="button" data-edit-person-id="${person.personId}">Upravit roli</button>
          <button class="secondary icon-action danger" type="button" data-delete-person-id="${person.personId}"
            data-company-id="${companyId}">Odebrat</button>
        </div>
      </div>`).join('');
  }

  function renderHistory(changes) {
    if (!changes.length) return '<div class="history-item">Historie zatím neobsahuje žádné události.</div>';
    return changes.map(change => `<div class="history-item"><strong>${escapeHtml(change.type || 'Změna')}</strong>
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
    personForm.querySelector('button[type="submit"]').textContent = 'Přiřadit';
    byId('person-cancel-edit').hidden = true;
  }

  return { init, search, openByRegistration };
}
