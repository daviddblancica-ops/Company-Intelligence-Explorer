import { api } from './api.js';
import { byId, escapeHtml, setServerStatus, showToast, updateText } from './ui.js';

export function initPeople({ audit, openCompany }) {
  const form = byId('people-form');
  const query = byId('people-query');
  const results = byId('people-results');
  const detail = byId('people-detail');
  let people = [];
  let selectedId = null;

  function init() {
    form.addEventListener('submit', event => {
      event.preventDefault();
      load(query.value);
    });
    byId('people-show-all').addEventListener('click', () => {
      query.value = '';
      load('');
    });
    results.addEventListener('click', event => {
      const button = event.target.closest('button[data-person-id]');
      if (!button) return;
      selectedId = Number(button.dataset.personId);
      render();
    });
    detail.addEventListener('click', event => {
      const button = event.target.closest('button[data-company-registration]');
      if (button) openCompany(button.dataset.companyRegistration);
    });
  }

  async function load(value = '') {
    const term = String(value || '').trim();
    try {
      people = await api.get(`/api/people?q=${encodeURIComponent(term)}`);
      setServerStatus(true);
      audit.addActivity('Registr lidi', `${people.length} osob odpovida dotazu "${term || 'vse'}".`, 'low');
      if (selectedId && !people.some(person => person.id === selectedId)) selectedId = null;
      render();
    } catch (error) {
      setServerStatus(false);
      showToast(error.status === 0 ? 'Server je offline.' : 'Registr lidi se nepodarilo nacist.');
      audit.addActivity('Chyba registru lidi', 'API vratilo chybu pri nacitani osob a vazeb.', 'critical');
    }
  }

  function render() {
    updateText('people-total', people.length);
    updateText('relationship-total', people.reduce((sum, person) => sum + (person.roleCount || 0), 0));
    if (!people.length) {
      results.innerHTML = '<div class="empty">Zadne osoby nebyly nalezeny. Prirad osobu v detailu firmy nebo importuj data s lidmi.</div>';
      renderDetail(null);
      return;
    }
    if (!selectedId || !people.some(person => person.id === selectedId)) selectedId = people[0].id;
    results.innerHTML = people.map(person => `
      <article class="person-card compact-person ${person.id === selectedId ? 'selected' : ''}">
        <div class="person-head"><div><h3>${escapeHtml(person.fullName)}</h3>
          <p>${person.companyCount || 0} firem · ${person.roleCount || 0} roli</p></div>
          <button class="secondary" type="button" data-person-id="${person.id}">Detail</button></div>
      </article>`).join('');
    renderDetail(people.find(person => person.id === selectedId));
  }

  function renderDetail(person) {
    if (!person) {
      detail.innerHTML = '<div class="empty detail-empty"><h3>Detail osoby</h3><p>Vyber osobu v registru.</p></div>';
      return;
    }
    detail.innerHTML = `
      <div class="detail-heading"><div><h3>${escapeHtml(person.fullName)}</h3><p>${escapeHtml(person.normalizedName || '')}</p></div></div>
      <div class="detail-section"><h4>Souhrn vazeb</h4>
        <div class="detail-row"><strong>Firmy</strong><span>${person.companyCount || 0}</span></div>
        <div class="detail-row"><strong>Role</strong><span>${person.roleCount || 0}</span></div>
      </div>
      <div class="detail-section"><h4>Firmy a role</h4>
        <div class="relationship-list">${renderCompanies(person.companies || [])}</div>
      </div>`;
  }

  function renderCompanies(companies) {
    if (!companies.length) return '<div class="history-item">Osoba zatim nema vazbu na zadnou firmu.</div>';
    return companies.map(company => `
      <div class="relationship-item"><div><strong>${escapeHtml(company.companyName || 'Firma')}</strong>
        <span>${escapeHtml(company.role || 'role neuvedena')} · ICO ${escapeHtml(company.registrationNumber || '-')}</span></div>
        <button class="secondary" type="button" data-company-registration="${escapeHtml(company.registrationNumber || '')}">Otevrit firmu</button>
      </div>`).join('');
  }

  return { init, load };
}
