package cz.companyintel.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
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
        assertThat(response.getBody()).contains("data-view=\"audit\"");
        assertThat(response.getBody()).contains("TODO list projektu");
        assertThat(response.getBody()).contains("Stav jadra");
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
    void exposesProjectTodoList() {
        ResponseEntity<String> tasks = restTemplate.getForEntity("/api/tasks", String.class);

        assertThat(tasks.getStatusCodeValue()).isEqualTo(200);
        assertThat(tasks.getBody()).contains("Stabilizovat jadro");
        assertThat(tasks.getBody()).contains("Rozsirit rychle vyhledavani");
        assertThat(tasks.getBody()).contains("\"title\":\"1. Stabilizovat jadro: health endpoint, chybove odpovedi, stav databaze\"");
        assertThat(tasks.getBody()).contains("\"title\":\"2. Pridat startup demo data pro firmy, osoby, vazby a audit\"");
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
    void returnsStructuredBadRequestForInvalidTask() {
        TaskRequest task = new TaskRequest();
        task.setTitle("");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/tasks", task, String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).contains("\"status\":400");
        assertThat(response.getBody()).contains("\"error\":\"Bad Request\"");
        assertThat(response.getBody()).contains("\"path\":\"/api/tasks\"");
    }
}
