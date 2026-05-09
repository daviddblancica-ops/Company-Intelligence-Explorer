export function createUi(dom) {
  function escapeHtml(value) {
    return String(value || '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }

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
    dom.toast.textContent = message;
    dom.toast.classList.add('visible');
  }

  function setServerStatus(online) {
    dom.serverStatus.classList.toggle('online', online);
    dom.serverStatus.classList.toggle('offline', !online);
    dom.statusTitle.textContent = online ? 'Server bezi' : 'Server offline';
  }

  function setSidebarCollapsed(collapsed) {
    dom.shell.classList.toggle('sidebar-collapsed', collapsed);
    dom.sidebarToggle.textContent = collapsed ? '›' : '‹';
    dom.sidebarToggle.setAttribute('aria-label', collapsed ? 'Rozbalit menu' : 'Zasunout menu');
    dom.sidebarToggle.setAttribute('title', collapsed ? 'Rozbalit menu' : 'Zasunout menu');
    localStorage.setItem('sidebarCollapsed', String(collapsed));
  }

  function showCurrentPage() {
    const allowedPages = dom.pages.map(page => page.id);
    const requestedPage = window.location.hash.replace('#', '') || 'system';
    const activePage = allowedPages.includes(requestedPage) ? requestedPage : 'system';

    dom.pages.forEach(page => {
      page.classList.toggle('active', page.id === activePage);
    });
    dom.pageLinks.forEach(link => {
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

  return {
    escapeHtml,
    formatDate,
    showToast,
    setServerStatus,
    setSidebarCollapsed,
    showCurrentPage,
    loadStoredArray,
    storeArray
  };
}
