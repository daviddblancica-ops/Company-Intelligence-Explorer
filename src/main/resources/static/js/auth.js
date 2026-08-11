import { api } from './api.js';
import { byId } from './ui.js';

export function initAuth() {
  const dialog = byId('login-dialog');
  const form = byId('login-form');
  const error = byId('login-error');
  const userName = byId('current-user-name');
  const userRole = byId('current-user-role');
  const logout = byId('logout-button');
  let initialLoginComplete = false;

  form.addEventListener('submit', async event => {
    event.preventDefault();
    error.textContent = '';
    const submit = event.submitter;
    submit.disabled = true;
    submit.textContent = 'Přihlašuji...';
    try {
      const data = new FormData(form);
      await api.get('/api/auth/session');
      await api.form('/api/auth/login', {
        username: String(data.get('username') || '').trim(),
        password: String(data.get('password') || '')
      });
      const session = await api.get('/api/auth/session');
      applySession(session);
      form.reset();
      dialog.close();
      document.dispatchEvent(new CustomEvent('cie:login-success', { detail: session }));
    } catch (requestError) {
      error.textContent = requestError.message || 'Přihlášení se nezdařilo.';
    } finally {
      submit.disabled = false;
      submit.textContent = 'Přihlásit se';
    }
  });

  logout.addEventListener('click', async () => {
    logout.disabled = true;
    try {
      await api.post('/api/auth/logout', {});
    } finally {
      window.location.reload();
    }
  });

  window.addEventListener('cie:authentication-required', () => {
    if (initialLoginComplete) window.location.reload();
  });

  async function requireLogin() {
    let session;
    try {
      session = await api.get('/api/auth/session');
    } catch (requestError) {
      error.textContent = 'Server neodpovídá. Zkontrolujte, zda je aplikace spuštěná.';
      showLogin();
      return waitForLogin();
    }
    if (!session.authenticated) {
      showLogin();
      return waitForLogin();
    }
    applySession(session);
    initialLoginComplete = true;
    return session;
  }

  function waitForLogin() {
    return new Promise(resolve => {
      document.addEventListener('cie:login-success', event => {
        initialLoginComplete = true;
        resolve(event.detail);
      }, { once: true });
    });
  }

  function showLogin() {
    document.body.classList.remove('auth-pending');
    if (!dialog.open) dialog.showModal();
    form.elements.username.focus();
  }

  function applySession(session) {
    const access = session.canAdmin ? 'admin' : session.canEdit ? 'editor' : 'viewer';
    document.body.dataset.access = access;
    document.body.classList.remove('auth-pending');
    userName.textContent = session.username || '';
    userRole.textContent = roleLabel(access);
  }

  function roleLabel(access) {
    return ({ admin: 'Administrátor', editor: 'Editor', viewer: 'Pouze čtení' })[access] || access;
  }

  return { requireLogin };
}
