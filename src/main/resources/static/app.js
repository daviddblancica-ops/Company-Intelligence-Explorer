import { initAudit } from './js/audit.js';
import { initAuth } from './js/auth.js';
import { initCompanies } from './js/companies.js';
import { initDashboard } from './js/dashboard.js';
import { initImports } from './js/imports.js';
import { initNavigation } from './js/navigation.js';
import { initPeople } from './js/people.js';
import { initTasks } from './js/tasks.js';

const auth = initAuth();
await auth.requireLogin();

const features = {};
const audit = initAudit();
audit.init();

const dashboard = initDashboard({ onError: audit.addActivity });
const navigation = initNavigation({
  onViewChange(view) {
    if (view === 'audit') audit.load(false);
    if (view === 'import' && features.imports) features.imports.loadRuns(false);
    if (view === 'tasks' && features.tasks) features.tasks.load(false);
  }
});

features.people = initPeople({
  audit,
  openCompany: registrationNumber => features.companies.openByRegistration(registrationNumber),
  onChanged: () => features.companies.search(document.getElementById('query').value)
});
features.companies = initCompanies({
  audit,
  navigation,
  onChanged: refreshOverview,
  onPeopleChanged: () => features.people.load(document.getElementById('people-query').value)
});
features.imports = initImports({
  audit,
  onChanged: refreshRecords
});
features.tasks = initTasks({ audit, onChanged: refreshOverview });

features.people.init();
features.companies.init();
features.imports.init();
features.tasks.init();
dashboard.start();

await Promise.allSettled([
  audit.loadTypes(),
  audit.load(false),
  features.imports.loadRuns(false),
  features.tasks.load(false),
  features.people.load(''),
  features.companies.search('')
]);

window.addEventListener('beforeunload', dashboard.stop);

async function refreshOverview() {
  await Promise.allSettled([dashboard.loadHealth(), dashboard.loadMetrics(), audit.load(false)]);
}

async function refreshRecords() {
  await Promise.allSettled([
    refreshOverview(),
    features.companies.search(document.getElementById('query').value),
    features.people.load(document.getElementById('people-query').value)
  ]);
}
