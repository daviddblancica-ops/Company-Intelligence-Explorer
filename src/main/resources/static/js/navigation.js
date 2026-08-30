const DEFAULT_VIEW = 'system';

export function initNavigation({ onViewChange = () => {} } = {}) {
  const shell = document.getElementById('app-shell');
  const toggle = document.getElementById('sidebar-toggle');
  const links = Array.from(document.querySelectorAll('[data-view-target]'));
  const views = Array.from(document.querySelectorAll('[data-view]'));
  const allowed = new Set(views.map(view => view.dataset.view));

  function setSidebarCollapsed(collapsed) {
    shell.classList.toggle('sidebar-collapsed', collapsed);
    toggle.textContent = collapsed ? '›' : '‹';
    toggle.setAttribute('aria-label', collapsed ? 'Rozbalit menu' : 'Zasunout menu');
    toggle.setAttribute('title', collapsed ? 'Rozbalit menu' : 'Zasunout menu');
    localStorage.setItem('sidebarCollapsed', String(collapsed));
  }

  function show(viewName, updateHash = true) {
    const target = allowed.has(viewName) ? viewName : DEFAULT_VIEW;
    views.forEach(view => view.classList.toggle('active', view.dataset.view === target));
    links.forEach(link => {
      const active = link.dataset.viewTarget === target;
      link.classList.toggle('active', active);
      if (link.closest('nav')) {
        active ? link.setAttribute('aria-current', 'page') : link.removeAttribute('aria-current');
      }
    });
    if (updateHash && window.location.hash !== `#${target}`) {
      window.history.pushState(null, '', `#${target}`);
    }
    window.scrollTo({ top: 0, behavior: 'auto' });
    onViewChange(target);
  }

  toggle.addEventListener('click', () => {
    setSidebarCollapsed(!shell.classList.contains('sidebar-collapsed'));
  });
  links.forEach(link => link.addEventListener('click', event => {
    event.preventDefault();
    show(link.dataset.viewTarget);
  }));
  window.addEventListener('hashchange', () => show(currentHashView(), false));
  window.addEventListener('popstate', () => show(currentHashView(), false));

  setSidebarCollapsed(localStorage.getItem('sidebarCollapsed') === 'true');
  show(currentHashView(), false);

  return { show };
}

function currentHashView() {
  return window.location.hash.replace('#', '') || DEFAULT_VIEW;
}
