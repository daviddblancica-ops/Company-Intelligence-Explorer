import { api } from './api.js';
import { byId, escapeHtml, setServerStatus, showToast, updateText } from './ui.js';

export function initPeople({ audit, openCompany, onChanged = async () => {} }) {
  const form = byId('people-form');
  const query = byId('people-query');
  const results = byId('people-results');
  const editDialog = byId('person-edit-dialog');
  const editForm = byId('person-edit-form');
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
    results.addEventListener('click', handleResultAction);
    editForm.addEventListener('submit', savePersonEdit);
    document.querySelectorAll('[data-close-dialog="person-edit-dialog"]').forEach(button => {
      button.addEventListener('click', () => editDialog.close());
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

  async function handleResultAction(event) {
    const detailButton = event.target.closest('button[data-person-id]');
    if (detailButton) {
      const id = Number(detailButton.dataset.personId);
      selectedId = selectedId === id ? null : id;
      render();
      return;
    }

    const companyButton = event.target.closest('button[data-company-registration]');
    if (companyButton) {
      openCompany(companyButton.dataset.companyRegistration);
      return;
    }

    const editButton = event.target.closest('button[data-edit-record-id]');
    if (editButton) {
      const person = people.find(item => item.id === Number(editButton.dataset.editRecordId));
      if (person) openEditDialog(person);
      return;
    }

    const deleteButton = event.target.closest('button[data-delete-record-id]');
    if (deleteButton) await deletePerson(Number(deleteButton.dataset.deleteRecordId));
  }

  function openEditDialog(person) {
    editForm.elements.id.value = person.id;
    editForm.elements.fullName.value = person.fullName || '';
    editForm.elements.dateOfBirth.value = person.dateOfBirth || '';
    editForm.elements.residenceAddress.value = person.residenceAddress || '';
    editForm.elements.note.value = person.note || '';
    editDialog.showModal();
    editForm.elements.fullName.focus();
  }

  async function savePersonEdit(event) {
    event.preventDefault();
    const data = new FormData(editForm);
    const id = Number(data.get('id'));
    const payload = {
      fullName: String(data.get('fullName') || '').trim(),
      dateOfBirth: data.get('dateOfBirth') || null,
      residenceAddress: String(data.get('residenceAddress') || '').trim(),
      note: String(data.get('note') || '').trim()
    };
    try {
      const updated = await api.put(`/api/people/${id}`, payload);
      people = people.map(person => person.id === updated.id ? updated : person);
      selectedId = updated.id;
      editDialog.close();
      render();
      showToast('Osobni zaznam byl upraven.');
      audit.addActivity('Uprava osoby', `${updated.fullName} byl upraven.`, 'warning');
      await onChanged();
    } catch (error) {
      showToast(error.message || 'Osobu se nepodarilo ulozit.');
    }
  }

  async function deletePerson(id) {
    const person = people.find(item => item.id === id);
    if (!person || !window.confirm(`Opravdu smazat osobu ${person.fullName}? Odstrani se i vsechny jeji vazby na firmy.`)) return;
    try {
      await api.delete(`/api/people/${id}`);
      people = people.filter(item => item.id !== id);
      selectedId = null;
      render();
      showToast('Osoba byla smazana z registru.');
      audit.addActivity('Smazani osoby', `${person.fullName} byl smazan z registru.`, 'critical');
      await onChanged();
    } catch (error) {
      showToast(error.message || 'Osobu se nepodarilo smazat.');
    }
  }

  function render() {
    updateText('people-total', people.length);
    updateText('relationship-total', people.reduce((sum, person) => sum + (person.roleCount || 0), 0));
    if (!people.length) {
      results.innerHTML = '<div class="empty">Zadne osoby nebyly nalezeny. Prirad osobu v detailu firmy nebo importuj data s lidmi.</div>';
      return;
    }
    results.innerHTML = people.map(person => renderPersonEntry(person)).join('');
  }

  function renderPersonEntry(person) {
    const expanded = person.id === selectedId;
    return `<div class="registry-entry">
      <article class="person-card compact-person ${expanded ? 'selected expanded' : ''}">
        <div class="person-head"><div><h3>${escapeHtml(person.fullName)}</h3>
          <p>${person.companyCount || 0} firem · ${person.roleCount || 0} roli</p></div>
          <button class="secondary" type="button" data-person-id="${person.id}" aria-expanded="${expanded}">${expanded ? 'Skryt' : 'Detail'}</button></div>
      </article>
      ${expanded ? renderDetail(person) : ''}
    </div>`;
  }

  function renderDetail(person) {
    return `<section class="inline-detail" aria-label="Detail osoby ${escapeHtml(person.fullName)}">
      <div class="detail-heading"><div><h3>${escapeHtml(person.fullName)}</h3><p>${escapeHtml(person.normalizedName || '')}</p></div>
        <div class="record-actions"><button class="secondary" type="button" data-edit-record-id="${person.id}">Upravit osobu</button>
          <button class="danger" type="button" data-delete-record-id="${person.id}">Smazat osobu</button></div></div>
      <div class="person-profile-grid">
        <div class="detail-section"><h4>Osobni udaje</h4>
          ${detailRow('Datum narozeni', formatBirthDate(person.dateOfBirth) || '-')}
          ${detailRow('Bydliste', person.residenceAddress || '-')}
          ${detailRow('Poznamka', person.note || '-')}
        </div>
        <div class="detail-section"><h4>Souhrn vazeb</h4>
          ${detailRow('Firmy', person.companyCount || 0)}${detailRow('Role', person.roleCount || 0)}
        </div>
      </div>
      <div class="detail-section"><h4>Firmy a role</h4>
        <div class="relationship-list">${renderCompanies(person.companies || [])}</div>
      </div>
    </section>`;
  }

  function renderCompanies(companies) {
    if (!companies.length) return '<div class="history-item">Osoba zatim nema vazbu na zadnou firmu.</div>';
    return companies.map(company => `
      <div class="relationship-item"><div><strong>${escapeHtml(company.companyName || 'Firma')}</strong>
        <span>${escapeHtml(company.role || 'role neuvedena')} · ICO ${escapeHtml(company.registrationNumber || '-')}</span></div>
        <button class="secondary" type="button" data-company-registration="${escapeHtml(company.registrationNumber || '')}">Otevrit firmu</button>
      </div>`).join('');
  }

  function detailRow(label, value) {
    return `<div class="detail-row"><strong>${escapeHtml(label)}</strong><span>${escapeHtml(value)}</span></div>`;
  }

  function formatBirthDate(value) {
    if (!value) return '';
    const date = new Date(`${value}T00:00:00`);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('cs-CZ');
  }

  return { init, load };
}
