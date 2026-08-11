let toastTimer;

export function byId(id) {
  const element = document.getElementById(id);
  if (!element) {
    throw new Error(`Missing UI element: #${id}`);
  }
  return element;
}

export function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

export function formatDate(value) {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? escapeHtml(value) : date.toLocaleString('cs-CZ');
}

export function showToast(message) {
  const toast = byId('toast');
  window.clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.add('visible');
  toastTimer = window.setTimeout(() => toast.classList.remove('visible'), 4500);
}

export function setServerStatus(online) {
  const status = byId('server-status');
  status.classList.toggle('online', online);
  status.classList.toggle('offline', !online);
  byId('status-title').textContent = online ? 'Server bezi' : 'Server offline';
}

export function updateText(id, value) {
  byId(id).textContent = String(value ?? '');
}

export function setButtonBusy(button, busy, busyLabel = 'Pracuji...') {
  if (busy) {
    button.dataset.originalLabel = button.textContent;
    button.textContent = busyLabel;
    button.disabled = true;
    return;
  }
  button.textContent = button.dataset.originalLabel || button.textContent;
  button.disabled = false;
  delete button.dataset.originalLabel;
}
