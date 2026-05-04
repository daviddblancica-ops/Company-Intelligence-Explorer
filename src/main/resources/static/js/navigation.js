export function initNavigation({ dom, ui }) {
  const sidebarCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
  ui.setSidebarCollapsed(sidebarCollapsed);

  dom.sidebarToggle.addEventListener('click', () => {
    ui.setSidebarCollapsed(!dom.shell.classList.contains('sidebar-collapsed'));
  });

  window.addEventListener('hashchange', ui.showCurrentPage);
}
