package cz.companyintel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class HomePageTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesHomePage() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);
        ResponseEntity<String> appScript = restTemplate.getForEntity("/app.js", String.class);
        ResponseEntity<String> companyScript = restTemplate.getForEntity("/js/companies.js", String.class);
        ResponseEntity<String> importScript = restTemplate.getForEntity("/js/imports.js", String.class);
        ResponseEntity<String> styles = restTemplate.getForEntity("/styles.css", String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("Company Intelligence Explorer");
        assertThat(response.getBody()).contains("Načíst z backendu");
        assertThat(response.getBody()).contains("type=\"module\" src=\"/app.js\"");
        assertThat(response.getBody()).contains("href=\"/styles.css\"");
        assertThat(response.getBody()).doesNotContain("<script>");
        assertThat(response.getBody()).contains("data-view-target=\"import\"");
        assertThat(response.getBody()).contains("data-import-tab=\"ares\"");
        assertThat(response.getBody()).contains("data-import-tab=\"history\"");
        assertThat(response.getBody()).contains("import-runs");
        assertThat(response.getBody()).contains("import-total-saved");
        assertThat(response.getBody()).contains("refresh-import-runs");
        assertThat(response.getBody()).contains("import-run-detail");
        assertThat(response.getBody()).contains("preview-json");
        assertThat(response.getBody()).contains("import-preview");
        assertThat(response.getBody()).contains("company-edit-dialog");
        assertThat(response.getBody()).contains("person-edit-dialog");
        assertThat(response.getBody()).contains("value=\"23143614\"");
        assertThat(response.getBody()).doesNotContain("seed-demo");
        assertThat(response.getBody()).contains("data-view-target=\"people\"");
        assertThat(response.getBody()).contains("data-view-target=\"notes\"");
        assertThat(response.getBody()).contains("data-view=\"audit\"");
        assertThat(response.getBody()).contains("data-view=\"notes\"");
        assertThat(response.getBody()).contains("audit-type-filter");
        assertThat(response.getBody()).contains("audit-query-filter");
        assertThat(response.getBody()).contains("export-audit");
        assertThat(response.getBody()).contains("audit-print-table");
        assertThat(response.getBody()).contains("Tisk A4 na šířku");
        assertThat(response.getBody()).contains("TODO list projektu");
        assertThat(response.getBody()).contains("Stav jádra");
        assertThat(response.getBody()).contains("dashboard-companies");
        assertThat(appScript.getStatusCodeValue()).isEqualTo(200);
        assertThat(appScript.getBody()).contains("initCompanies");
        assertThat(companyScript.getStatusCodeValue()).isEqualTo(200);
        assertThat(companyScript.getBody()).contains("Přiřadit");
        assertThat(companyScript.getBody()).contains("data-edit-person-id");
        assertThat(importScript.getStatusCodeValue()).isEqualTo(200);
        assertThat(importScript.getBody()).contains("back-to-import-runs");
        assertThat(styles.getStatusCodeValue()).isEqualTo(200);
        assertThat(styles.getBody()).contains(".import-tabs");
        assertThat(styles.getBody()).contains("size: A4 landscape");
        assertThat(appScript.getBody()).contains("initAudit");
    }

    @Test
    void exposesAuditEventsAndPersonAssignmentApi() {
        CompanyRequest company = new CompanyRequest();
        company.setName("Audit API Test s.r.o.");
        company.setRegistrationNumber("12345001");
        company.setCountry("CZ");
        company.setLegalForm("s.r.o.");

        ResponseEntity<CompanyResponse> created = restTemplate.postForEntity("/api/companies", company, CompanyResponse.class);
        assertThat(created.getStatusCodeValue()).isEqualTo(201);

        PersonAssignmentRequest person = new PersonAssignmentRequest();
        person.setFullName("Pavel Audit");
        person.setRole("kontrolor");
        ResponseEntity<CompanyResponse> assigned = restTemplate.postForEntity(
                "/api/companies/" + created.getBody().getId() + "/people",
                new HttpEntity<PersonAssignmentRequest>(person),
                CompanyResponse.class);

        assertThat(assigned.getStatusCodeValue()).isEqualTo(200);
        assertThat(assigned.getBody().getPeople()).hasSize(1);

        ResponseEntity<String> audit = restTemplate.getForEntity("/api/audit?limit=20", String.class);
        assertThat(audit.getStatusCodeValue()).isEqualTo(200);
        assertThat(audit.getBody()).contains("PERSON_ASSIGNED");
    }

    @Test
    void archivesAuditEventsAndExposesEventTypes() {
        CompanyRequest company = new CompanyRequest();
        company.setName("Archive Audit Test s.r.o.");
        company.setRegistrationNumber("12345011");
        company.setCountry("CZ");
        company.setLegalForm("s.r.o.");
        restTemplate.postForEntity("/api/companies", company, CompanyResponse.class);

        ResponseEntity<String> types = restTemplate.getForEntity("/api/audit/types", String.class);
        ResponseEntity<String> active = restTemplate.getForEntity("/api/audit?archived=false&limit=20", String.class);

        assertThat(types.getStatusCodeValue()).isEqualTo(200);
        assertThat(types.getBody()).contains("CREATED");
        assertThat(active.getStatusCodeValue()).isEqualTo(200);
        assertThat(active.getBody()).contains("\"archived\":false");

        Long eventId = firstId(active.getBody());
        AuditArchiveRequest request = new AuditArchiveRequest();
        request.setArchived(true);
        ResponseEntity<String> archived = restTemplate.postForEntity(
                "/api/audit/" + eventId + "/archive",
                new HttpEntity<AuditArchiveRequest>(request),
                String.class);
        ResponseEntity<String> archive = restTemplate.getForEntity("/api/audit?archived=true&limit=20", String.class);

        assertThat(archived.getStatusCodeValue()).isEqualTo(200);
        assertThat(archived.getBody()).contains("\"archived\":true");
        assertThat(archive.getBody()).contains("\"id\":" + eventId);
    }

    @Test
    void filtersExportsAndBulkArchivesAuditEvents() {
        CompanyRequest company = new CompanyRequest();
        company.setName("Filtered Audit Systems s.r.o.");
        company.setRegistrationNumber("12345009");
        company.setCountry("CZ");
        company.setLegalForm("s.r.o.");
        ResponseEntity<CompanyResponse> created = restTemplate.postForEntity(
                "/api/companies", company, CompanyResponse.class);
        Long companyId = created.getBody().getId();

        String filterUrl = "/api/audit?companyId=" + companyId
                + "&type=CREATED&severity=INFO&query=12345009"
                + "&from=2020-01-01&to=2099-12-31&limit=20";
        ResponseEntity<String> filtered = restTemplate.getForEntity(filterUrl, String.class);
        ResponseEntity<String> future = restTemplate.getForEntity(
                "/api/audit?companyId=" + companyId + "&from=2999-01-01", String.class);
        ResponseEntity<String> export = restTemplate.getForEntity(
                "/api/audit/export.csv?companyId=" + companyId, String.class);

        assertThat(filtered.getStatusCodeValue()).isEqualTo(200);
        assertThat(filtered.getBody()).contains("Filtered Audit Systems s.r.o.");
        assertThat(filtered.getBody()).contains("\"severity\":\"INFO\"");
        assertThat(future.getBody()).isEqualTo("[]");
        assertThat(export.getStatusCodeValue()).isEqualTo(200);
        assertThat(export.getHeaders().getContentType().toString()).startsWith("text/csv");
        assertThat(export.getHeaders().getFirst("Content-Disposition")).contains("company-intelligence-audit.csv");
        assertThat(export.getBody()).contains("čas,důležitost,typ,subjekt");
        assertThat(export.getBody()).contains("Filtered Audit Systems s.r.o.");

        Long eventId = firstId(filtered.getBody());
        AuditBulkArchiveRequest archiveRequest = new AuditBulkArchiveRequest();
        archiveRequest.setIds(Collections.singletonList(eventId));
        archiveRequest.setArchived(true);
        ResponseEntity<String> archived = restTemplate.postForEntity(
                "/api/audit/archive", archiveRequest, String.class);
        ResponseEntity<String> archive = restTemplate.getForEntity(
                "/api/audit?companyId=" + companyId + "&archived=true", String.class);

        assertThat(archived.getStatusCodeValue()).isEqualTo(200);
        assertThat(archived.getBody()).contains("\"archived\":true");
        assertThat(archive.getBody()).contains("\"id\":" + eventId);

        archiveRequest.setArchived(false);
        restTemplate.postForEntity("/api/audit/archive", archiveRequest, String.class);
    }

    @Test
    void rejectsUnsupportedAuditSeverity() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/audit?severity=urgent", String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).contains("Nepodporovaná důležitost auditu");
    }

    @Test
    void tracksImportRunsWithRowErrors() {
        String csv = "name,registrationNumber,country,legalForm,people\n"
                + "Tracked Import s.r.o.,88112233,CZ,s.r.o.,Jana Dobra|jednatelka\n"
                + "Broken row without enough columns\n";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));

        ResponseEntity<String> result = restTemplate.postForEntity(
                "/api/import/csv",
                new HttpEntity<String>(csv, headers),
                String.class);
        ResponseEntity<String> runs = restTemplate.getForEntity("/api/import/runs", String.class);

        assertThat(result.getStatusCodeValue()).isEqualTo(200);
        assertThat(result.getBody()).contains("\"imported\":1");
        assertThat(result.getBody()).contains("\"failed\":1");
        assertThat(runs.getStatusCodeValue()).isEqualTo(200);
        assertThat(runs.getBody()).contains("\"sourceType\":\"CSV\"");
        assertThat(runs.getBody()).contains("\"status\":\"PARTIAL\"");
        assertThat(runs.getBody()).contains("\"rowNumber\":3");
        assertThat(runs.getBody()).contains("Očekáváno 5 sloupců CSV");
    }

    @Test
    void exposesSingleImportRunDetail() {
        String csv = "name,registrationNumber,country,legalForm,people\n"
                + "Detail Import s.r.o.,88112234,CZ,s.r.o.,Jana Detail|jednatelka\n"
                + "Broken detail row\n";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));

        ResponseEntity<String> result = restTemplate.postForEntity(
                "/api/import/csv",
                new HttpEntity<String>(csv, headers),
                String.class);
        Long runId = firstValue(result.getBody(), "runId");
        ResponseEntity<String> detail = restTemplate.getForEntity("/api/import/runs/" + runId, String.class);

        assertThat(detail.getStatusCodeValue()).isEqualTo(200);
        assertThat(detail.getBody()).contains("\"id\":" + runId);
        assertThat(detail.getBody()).contains("\"sourceType\":\"CSV\"");
        assertThat(detail.getBody()).contains("\"status\":\"PARTIAL\"");
        assertThat(detail.getBody()).contains("\"importedRows\":1");
        assertThat(detail.getBody()).contains("\"failedRows\":1");
        assertThat(detail.getBody()).contains("\"rowNumber\":3");
        assertThat(detail.getBody()).contains("Broken detail row");
    }

    @Test
    void recordsImportRunsInAuditLog() {
        String csv = "name,registrationNumber,country,legalForm,people\n"
                + "Audit Import s.r.o.,88112235,CZ,s.r.o.,Jana Audit|jednatelka\n"
                + "Broken audit row\n";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));

        ResponseEntity<String> result = restTemplate.postForEntity(
                "/api/import/csv",
                new HttpEntity<String>(csv, headers),
                String.class);
        Long runId = firstValue(result.getBody(), "runId");
        ResponseEntity<String> audit = restTemplate.getForEntity("/api/audit?type=IMPORT_PARTIAL&limit=20", String.class);

        assertThat(result.getStatusCodeValue()).isEqualTo(200);
        assertThat(audit.getStatusCodeValue()).isEqualTo(200);
        assertThat(audit.getBody()).contains("\"type\":\"IMPORT_PARTIAL\"");
        assertThat(audit.getBody()).contains("\"severity\":\"WARNING\"");
        assertThat(audit.getBody()).contains("\"importRunId\":" + runId);
        assertThat(audit.getBody()).contains("Importní běh #" + runId);
        assertThat(audit.getBody()).contains("importováno 1");
        assertThat(audit.getBody()).contains("chybně 1");
    }

    @Test
    void previewsCsvImportWithoutSavingCompanies() {
        String csv = "name,registrationNumber,country,legalForm,people\n"
                + "Preview API s.r.o.,91919191,CZ,s.r.o.,Pavel Preview|jednatel\n"
                + "Broken row\n";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv"));

        ResponseEntity<String> preview = restTemplate.postForEntity(
                "/api/import/preview/csv",
                new HttpEntity<String>(csv, headers),
                String.class);
        ResponseEntity<String> companies = restTemplate.getForEntity("/api/companies/search?q=Preview%20API", String.class);

        assertThat(preview.getStatusCodeValue()).isEqualTo(200);
        assertThat(preview.getBody()).contains("\"sourceType\":\"CSV\"");
        assertThat(preview.getBody()).contains("\"validRows\":1");
        assertThat(preview.getBody()).contains("\"invalidRows\":1");
        assertThat(preview.getBody()).contains("Očekáváno 5 sloupců CSV");
        assertThat(companies.getBody()).doesNotContain("Preview API s.r.o.");
    }

    @Test
    void exposesProjectTodoList() {
        ResponseEntity<String> tasks = restTemplate.getForEntity("/api/tasks", String.class);

        assertThat(tasks.getStatusCodeValue()).isEqualTo(200);
        assertThat(tasks.getBody()).contains("Stabilizovat jádro");
        assertThat(tasks.getBody()).contains("Rozšířit rychlé vyhledávání");
        assertThat(tasks.getBody()).contains("\"title\":\"1. Stabilizovat jádro: health endpoint, chybové odpovědi, stav databáze\"");
        assertThat(tasks.getBody()).contains("\"title\":\"2. Ověřit import reálné firmy z ARES podle IČO\"");
        assertThat(tasks.getBody()).contains("\"title\":\"3. Dodělat registr lidí a detail osoby s vazbami na firmy\"");
        assertThat(tasks.getBody()).contains("\"title\":\"4. Rozšířit rychlé vyhledávání podle firmy, IČO, osoby a role\"");
        assertThat(tasks.getBody()).contains("\"title\":\"5. Posílit audit: filtry, typy událostí, archiv a tiskový výpis\"");
        assertThat(tasks.getBody()).contains("\"title\":\"6. Přidat historii importních běhů včetně chybných řádků\"");
        assertThat(tasks.getBody()).contains("\"done\":true");
    }

    @Test
    void exposesHealthStatus() {
        ResponseEntity<String> health = restTemplate.getForEntity("/api/health", String.class);

        assertThat(health.getStatusCodeValue()).isEqualTo(200);
        assertThat(health.getBody()).contains("\"status\":\"UP\"");
        assertThat(health.getBody()).contains("\"database\":\"UP\"");
        assertThat(health.getBody()).contains("\"tasks\"");
    }

    @Test
    void exposesDashboardMetrics() {
        ResponseEntity<String> dashboard = restTemplate.getForEntity("/api/dashboard", String.class);

        assertThat(dashboard.getStatusCodeValue()).isEqualTo(200);
        assertThat(firstValue(dashboard.getBody(), "companies")).isGreaterThanOrEqualTo(0L);
        assertThat(firstValue(dashboard.getBody(), "people")).isGreaterThanOrEqualTo(0L);
        assertThat(firstValue(dashboard.getBody(), "relationships")).isGreaterThanOrEqualTo(0L);
        assertThat(dashboard.getBody()).contains("\"watchlisted\"");
        assertThat(dashboard.getBody()).contains("\"auditEvents\"");
        assertThat(dashboard.getBody()).contains("\"importRuns\"");
    }

    @Test
    void doesNotSeedLegacyDemoRecords() {
        ResponseEntity<String> companies = restTemplate.getForEntity("/api/companies/search?q=70010001", String.class);
        ResponseEntity<String> audit = restTemplate.getForEntity("/api/audit?type=DEMO_DATA&limit=20", String.class);

        assertThat(companies.getStatusCodeValue()).isEqualTo(200);
        assertThat(companies.getBody()).isEqualTo("[]");
        assertThat(audit.getBody()).isEqualTo("[]");
    }

    @Test
    void exposesPeopleRegistryWithCompanyRelationships() {
        CompanyRequest company = new CompanyRequest();
        company.setName("People Registry Test s.r.o.");
        company.setRegistrationNumber("12345012");
        company.setCountry("CZ");
        company.setLegalForm("s.r.o.");
        ResponseEntity<CompanyResponse> created = restTemplate.postForEntity(
                "/api/companies", company, CompanyResponse.class);
        PersonAssignmentRequest assignment = new PersonAssignmentRequest();
        assignment.setFullName("Michaela Registry");
        assignment.setRole("jednatelka");
        ResponseEntity<CompanyResponse> assigned = restTemplate.postForEntity(
                "/api/companies/" + created.getBody().getId() + "/people",
                new HttpEntity<PersonAssignmentRequest>(assignment),
                CompanyResponse.class);

        assertThat(created.getStatusCodeValue()).isEqualTo(201);
        assertThat(assigned.getStatusCodeValue()).isEqualTo(200);
        assertThat(assigned.getBody().getPeople()).hasSize(1);

        ResponseEntity<String> people = restTemplate.getForEntity("/api/people?q=michaela", String.class);

        assertThat(people.getStatusCodeValue()).isEqualTo(200);
        assertThat(people.getBody()).contains("Michaela Registry");
        assertThat(people.getBody()).contains("People Registry Test s.r.o.");
        assertThat(people.getBody()).contains("jednatelka");

        Long personId = firstId(people.getBody());
        ResponseEntity<String> detail = restTemplate.getForEntity("/api/people/" + personId, String.class);

        assertThat(detail.getStatusCodeValue()).isEqualTo(200);
        assertThat(detail.getBody()).contains("\"companyCount\":1");
        assertThat(detail.getBody()).contains("\"roleCount\":1");
        assertThat(detail.getBody()).contains("People Registry Test s.r.o.");
    }

    @Test
    void updatesAndDeletesCompanyThroughApiWhileKeepingAudit() {
        CompanyRequest company = new CompanyRequest();
        company.setName("Company CRUD API s.r.o.");
        company.setRegistrationNumber("12345021");
        company.setCountry("CZ");
        company.setLegalForm("s.r.o.");
        ResponseEntity<CompanyResponse> created = restTemplate.postForEntity(
                "/api/companies", company, CompanyResponse.class);

        CompanyUpdateRequest update = new CompanyUpdateRequest();
        update.setName("Company CRUD API a.s.");
        update.setRegistrationNumber("12345022");
        update.setCountry("CZ");
        update.setLegalForm("a.s.");
        update.setAddress("Praha 2");
        update.setDataSource("MANUAL");
        ResponseEntity<CompanyResponse> updated = restTemplate.exchange(
                "/api/companies/" + created.getBody().getId(),
                HttpMethod.PUT,
                new HttpEntity<CompanyUpdateRequest>(update),
                CompanyResponse.class);
        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/companies/" + created.getBody().getId(),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);
        ResponseEntity<String> missing = restTemplate.getForEntity(
                "/api/companies/" + created.getBody().getId(), String.class);
        ResponseEntity<String> audit = restTemplate.getForEntity(
                "/api/audit?query=12345022&limit=20", String.class);

        assertThat(updated.getStatusCodeValue()).isEqualTo(200);
        assertThat(updated.getBody().getName()).isEqualTo("Company CRUD API a.s.");
        assertThat(updated.getBody().getRegistrationNumber()).isEqualTo("12345022");
        assertThat(deleted.getStatusCodeValue()).isEqualTo(204);
        assertThat(missing.getStatusCodeValue()).isEqualTo(404);
        assertThat(audit.getBody()).contains("COMPANY_DELETED");
        assertThat(audit.getBody()).contains("Company CRUD API a.s.");
    }

    @Test
    void updatesAndDeletesPersonThroughApi() {
        CompanyRequest company = new CompanyRequest();
        company.setName("Person CRUD API s.r.o.");
        company.setRegistrationNumber("12345023");
        company.setCountry("CZ");
        company.setLegalForm("s.r.o.");
        ResponseEntity<CompanyResponse> created = restTemplate.postForEntity(
                "/api/companies", company, CompanyResponse.class);
        PersonAssignmentRequest assignment = new PersonAssignmentRequest();
        assignment.setFullName("Person CRUD Original");
        assignment.setRole("jednatel");
        ResponseEntity<CompanyResponse> assigned = restTemplate.postForEntity(
                "/api/companies/" + created.getBody().getId() + "/people",
                assignment,
                CompanyResponse.class);
        Long personId = assigned.getBody().getPeople().get(0).getPersonId();

        PersonUpdateRequest update = new PersonUpdateRequest();
        update.setFullName("Person CRUD Updated");
        update.setDateOfBirth(java.time.LocalDate.of(1990, 1, 2));
        update.setResidenceAddress("Usti nad Labem");
        update.setNote("API test");
        ResponseEntity<PersonResponse> updated = restTemplate.exchange(
                "/api/people/" + personId,
                HttpMethod.PUT,
                new HttpEntity<PersonUpdateRequest>(update),
                PersonResponse.class);
        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/people/" + personId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class);
        ResponseEntity<String> missing = restTemplate.getForEntity(
                "/api/people/" + personId, String.class);
        ResponseEntity<CompanyResponse> companyAfterDelete = restTemplate.getForEntity(
                "/api/companies/" + created.getBody().getId(), CompanyResponse.class);

        assertThat(updated.getStatusCodeValue()).isEqualTo(200);
        assertThat(updated.getBody().getFullName()).isEqualTo("Person CRUD Updated");
        assertThat(updated.getBody().getDateOfBirth()).isEqualTo(java.time.LocalDate.of(1990, 1, 2));
        assertThat(updated.getBody().getResidenceAddress()).isEqualTo("Usti nad Labem");
        assertThat(deleted.getStatusCodeValue()).isEqualTo(204);
        assertThat(missing.getStatusCodeValue()).isEqualTo(404);
        assertThat(companyAfterDelete.getBody().getPeople()).isEmpty();
    }

    @Test
    void returnsStructuredBadRequestForInvalidTask() {
        TaskRequest task = new TaskRequest();
        task.setTitle("");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/tasks", task, String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).contains("\"status\":400");
        assertThat(response.getBody()).contains("\"error\":\"Bad Request\"");
        assertThat(response.getBody()).contains("\"path\":\"/api/tasks\"");
    }

    private Long firstId(String json) {
        Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(json);
        assertThat(matcher.find()).isTrue();
        return Long.valueOf(matcher.group(1));
    }

    private Long firstValue(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\":(\\d+)").matcher(json);
        assertThat(matcher.find()).isTrue();
        return Long.valueOf(matcher.group(1));
    }
}
