package cz.companyintel.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HomePageTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void servesHomePage() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("Company Intelligence Explorer");
        assertThat(response.getBody()).contains("Nacist z backendu");
        assertThat(response.getBody()).contains("Priradit");
        assertThat(response.getBody()).contains("data-view-target=\"import\"");
        assertThat(response.getBody()).contains("import-runs");
        assertThat(response.getBody()).contains("data-view-target=\"people\"");
        assertThat(response.getBody()).contains("data-view=\"audit\"");
        assertThat(response.getBody()).contains("audit-type-filter");
        assertThat(response.getBody()).contains("TODO list projektu");
        assertThat(response.getBody()).contains("Stav jadra");
        assertThat(response.getBody()).contains("dashboard-companies");
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
        ResponseEntity<String> types = restTemplate.getForEntity("/api/audit/types", String.class);
        ResponseEntity<String> active = restTemplate.getForEntity("/api/audit?archived=false&limit=20", String.class);

        assertThat(types.getStatusCodeValue()).isEqualTo(200);
        assertThat(types.getBody()).contains("DEMO_DATA");
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
        assertThat(runs.getBody()).contains("Expected 5 CSV columns");
    }

    @Test
    void exposesProjectTodoList() {
        ResponseEntity<String> tasks = restTemplate.getForEntity("/api/tasks", String.class);

        assertThat(tasks.getStatusCodeValue()).isEqualTo(200);
        assertThat(tasks.getBody()).contains("Stabilizovat jadro");
        assertThat(tasks.getBody()).contains("Rozsirit rychle vyhledavani");
        assertThat(tasks.getBody()).contains("\"title\":\"1. Stabilizovat jadro: health endpoint, chybove odpovedi, stav databaze\"");
        assertThat(tasks.getBody()).contains("\"title\":\"2. Pridat startup demo data pro firmy, osoby, vazby a audit\"");
        assertThat(tasks.getBody()).contains("\"title\":\"3. Dodelat registr lidi a detail osoby s vazbami na firmy\"");
        assertThat(tasks.getBody()).contains("\"title\":\"4. Rozsirit rychle vyhledavani podle firmy, ICO, osoby a role\"");
        assertThat(tasks.getBody()).contains("\"title\":\"5. Posilit audit: filtry, typy udalosti, archiv a tiskovy vypis\"");
        assertThat(tasks.getBody()).contains("\"title\":\"6. Pridat historii importnich behu vcetne chybovych radku\"");
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
        assertThat(dashboard.getBody()).contains("\"companies\":3");
        assertThat(dashboard.getBody()).contains("\"people\":4");
        assertThat(dashboard.getBody()).contains("\"relationships\":4");
        assertThat(dashboard.getBody()).contains("\"watchlisted\":1");
        assertThat(dashboard.getBody()).contains("\"auditEvents\"");
        assertThat(dashboard.getBody()).contains("\"importRuns\"");
    }

    @Test
    void startsWithDemoCompaniesPeopleAndAudit() {
        ResponseEntity<String> companies = restTemplate.getForEntity("/api/companies/search?q=", String.class);
        ResponseEntity<String> audit = restTemplate.getForEntity("/api/audit?limit=20", String.class);
        ResponseEntity<String> health = restTemplate.getForEntity("/api/health", String.class);

        assertThat(companies.getStatusCodeValue()).isEqualTo(200);
        assertThat(companies.getBody()).contains("Atlas Data Lab s.r.o.");
        assertThat(companies.getBody()).contains("Michaela Cerna");
        assertThat(companies.getBody()).contains("watchlisted\":true");
        assertThat(audit.getBody()).contains("DEMO_DATA");
        assertThat(health.getBody()).contains("\"companies\":3");
        assertThat(health.getBody()).contains("\"people\":4");
    }

    @Test
    void exposesPeopleRegistryWithCompanyRelationships() {
        ResponseEntity<String> people = restTemplate.getForEntity("/api/people?q=michaela", String.class);

        assertThat(people.getStatusCodeValue()).isEqualTo(200);
        assertThat(people.getBody()).contains("Michaela Cerna");
        assertThat(people.getBody()).contains("Atlas Data Lab s.r.o.");
        assertThat(people.getBody()).contains("jednatelka");

        Long personId = firstId(people.getBody());
        ResponseEntity<String> detail = restTemplate.getForEntity("/api/people/" + personId, String.class);

        assertThat(detail.getStatusCodeValue()).isEqualTo(200);
        assertThat(detail.getBody()).contains("\"companyCount\":1");
        assertThat(detail.getBody()).contains("\"roleCount\":1");
        assertThat(detail.getBody()).contains("Atlas Data Lab s.r.o.");
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
}
