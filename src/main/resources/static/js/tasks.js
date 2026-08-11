import { api } from './api.js';
import { byId, escapeHtml, formatDate, showToast, updateText } from './ui.js';

export function initTasks({ audit, onChanged = async () => {} }) {
  const form = byId('task-form');
  const list = byId('task-list');
  const archiveList = byId('task-archive-list');
  const archiveBox = byId('task-archive-box');
  const toggleArchive = byId('toggle-task-archive');
  let tasks = [];
  let archivedTasks = [];
  let archiveVisible = false;

  function init() {
    form.addEventListener('submit', createTask);
    byId('refresh-tasks').addEventListener('click', () => load(true));
    toggleArchive.addEventListener('click', () => {
      archiveVisible = !archiveVisible;
      archiveBox.hidden = !archiveVisible;
      toggleArchive.textContent = archiveVisible ? 'Skryt archiv' : 'Zobrazit archiv';
      if (archiveVisible) loadArchive();
    });
    list.addEventListener('change', event => {
      if (event.target.matches('[data-task-done]')) {
        patch(`/api/tasks/${event.target.dataset.taskDone}/done`, { done: event.target.checked });
      }
    });
    list.addEventListener('click', handleActiveAction);
    archiveList.addEventListener('click', event => {
      const button = event.target.closest('[data-task-restore]');
      if (button) patch(`/api/tasks/${button.dataset.taskRestore}/archive`, { archived: false });
    });
  }

  async function createTask(event) {
    event.preventDefault();
    const title = byId('task-title').value.trim();
    if (!title) return showToast('Vypln nazev ukolu.');
    await save('/api/tasks', 'POST', {
      title,
      segment: byId('task-segment').value,
      priority: byId('task-priority').value
    });
    byId('task-title').value = '';
  }

  async function handleActiveAction(event) {
    const edit = event.target.closest('[data-task-edit]');
    const archive = event.target.closest('[data-task-archive]');
    if (edit) {
      const task = tasks.find(item => String(item.id) === edit.dataset.taskEdit);
      const title = task ? window.prompt('Upravit ukol', task.title) : null;
      if (title !== null) await save(`/api/tasks/${task.id}`, 'PUT', { title, segment: task.segment, priority: task.priority });
    }
    if (archive) await patch(`/api/tasks/${archive.dataset.taskArchive}/archive`, { archived: true });
  }

  async function load(showMessage = false) {
    try {
      tasks = await api.get('/api/tasks');
      render();
      if (showMessage) showToast('TODO list nacten z backendu.');
    } catch (error) {
      showToast('TODO list se nepodarilo nacist.');
      audit.addActivity('TODO chyba', 'API pro ukoly neodpovedelo.', 'warning');
    }
  }

  async function loadArchive() {
    try {
      archivedTasks = await api.get('/api/tasks?archived=true');
      renderArchive();
    } catch (error) {
      showToast('Archiv ukolu se nepodarilo nacist.');
    }
  }

  async function save(url, method, payload) {
    try {
      method === 'POST' ? await api.post(url, payload) : await api.put(url, payload);
      showToast(method === 'POST' ? 'Ukol byl pridan.' : 'Ukol byl upraven.');
      await load(false);
      await onChanged();
    } catch (error) {
      showToast('Ukol se nepodarilo ulozit.');
    }
  }

  async function patch(url, payload) {
    try {
      await api.patch(url, payload);
      showToast('Stav ukolu byl ulozen.');
      await load(false);
      if (archiveVisible) await loadArchive();
      await onChanged();
    } catch (error) {
      showToast('Stav ukolu se nepodarilo ulozit.');
    }
  }

  function render() {
    const done = tasks.filter(task => task.done).length;
    updateText('task-total', tasks.length);
    updateText('task-done', done);
    updateText('task-open', tasks.length - done);
    list.innerHTML = tasks.length ? tasks.map(task => renderItem(task, false)).join('')
      : '<div class="task-empty">TODO list je prazdny. Pridej prvni ukol.</div>';
  }

  function renderArchive() {
    archiveList.innerHTML = archivedTasks.length ? archivedTasks.map(task => renderItem(task, true)).join('')
      : '<div class="task-empty">Archiv zatim neobsahuje zadne ukoly.</div>';
  }

  function renderItem(task, archived) {
    const priority = String(task.priority || 'MEDIUM').toLowerCase();
    const action = archived
      ? `<button class="secondary" type="button" data-task-restore="${task.id}">Obnovit</button>`
      : `<button class="secondary" type="button" data-task-edit="${task.id}">Upravit</button>
         <button class="secondary" type="button" data-task-archive="${task.id}">Archiv</button>`;
    return `<div class="task-item ${task.done ? 'done' : ''}">
      <input type="checkbox" data-task-done="${task.id}" ${task.done ? 'checked' : ''} ${archived ? 'disabled' : ''} aria-label="Hotovo">
      <div><strong>${escapeHtml(task.title)}</strong><div class="task-meta"><span>${escapeHtml(task.segment)}</span>
        <span class="task-priority ${priority}">${priorityLabel(task.priority)}</span><span>${formatDate(task.updatedAt)}</span></div></div>
      <div class="task-actions">${action}</div></div>`;
  }

  function priorityLabel(value) {
    return ({ HIGH: 'vysoka', LOW: 'nizka' })[String(value || '').toUpperCase()] || 'stredni';
  }

  return { init, load };
}
