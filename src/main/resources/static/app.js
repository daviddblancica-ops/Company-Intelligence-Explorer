import { demoCompanies, sampleCsv, sampleJson } from './js/demo-data.js';
import { getDomRefs } from './js/dom.js';
import { createState } from './js/state.js';
import { createUi } from './js/ui.js';
import { initAudit } from './js/audit.js';
import { initCompanies } from './js/companies.js';
import { initImports } from './js/imports.js';
import { initNavigation } from './js/navigation.js';

const dom = getDomRefs();
const state = createState();
const ui = createUi(dom);
const audit = initAudit({ dom, state, ui });
const companies = initCompanies({ dom, state, ui, audit });

initNavigation({ dom, ui });
initImports({ dom, state, ui, audit, companies, demoCompanies, sampleCsv, sampleJson });

ui.showCurrentPage();
audit.renderAuditLog();
audit.loadBackendAudit(false);
companies.search('');
