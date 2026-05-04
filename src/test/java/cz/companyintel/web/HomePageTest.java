package cz.companyintel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
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

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("Company Intelligence Explorer");
        assertThat(response.getBody()).contains("Nacist z backendu");
        assertThat(response.getBody()).contains("/app.js");
    }

    @Test
    void servesApplicationScript() {
        ResponseEntity<String> response = restTemplate.getForEntity("/app.js", String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("./js/audit.js");
        assertThat(response.getBody()).contains("./js/companies.js");
    }

    @Test
    void servesSegmentedApplicationModules() {
        ResponseEntity<String> audit = restTemplate.getForEntity("/js/audit.js", String.class);
        ResponseEntity<String> companies = restTemplate.getForEntity("/js/companies.js", String.class);

        assertThat(audit.getStatusCodeValue()).isEqualTo(200);
        assertThat(audit.getBody()).contains("ARCHIVED_AUDIT_IDS_KEY");
        assertThat(companies.getStatusCodeValue()).isEqualTo(200);
        assertThat(companies.getBody()).contains("Priradit");
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
    void updatesAndDeletesPersonAssignmentApi() throws Exception {
        CompanyRequest company = new CompanyRequest();
        company.setName("People Edit API Test s.r.o.");
        company.setRegistrationNumber("12345003");
        company.setCountry("CZ");
        company.setLegalForm("s.r.o.");

        ResponseEntity<CompanyResponse> created = restTemplate.postForEntity("/api/companies", company, CompanyResponse.class);
        assertThat(created.getStatusCodeValue()).isEqualTo(201);

        PersonAssignmentRequest person = new PersonAssignmentRequest();
        person.setFullName("Jana Edit");
        person.setRole("analyticka");
        ResponseEntity<CompanyResponse> assigned = restTemplate.postForEntity(
                "/api/companies/" + created.getBody().getId() + "/people",
                new HttpEntity<PersonAssignmentRequest>(person),
                CompanyResponse.class);
        Long personId = assigned.getBody().getPeople().get(0).getPersonId();

        mockMvc.perform(patch("/api/companies/" + created.getBody().getId() + "/people/" + personId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"jednatelka\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jednatelka")));

        mockMvc.perform(delete("/api/companies/" + created.getBody().getId() + "/people/" + personId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"people\":[]")));
    }

    @Test
    void rejectsBlankPersonAssignmentRequest() {
        CompanyRequest company = new CompanyRequest();
        company.setName("Blank API Test s.r.o.");
        company.setRegistrationNumber("12345002");
        company.setCountry("CZ");
        company.setLegalForm("s.r.o.");

        ResponseEntity<CompanyResponse> created = restTemplate.postForEntity("/api/companies", company, CompanyResponse.class);
        assertThat(created.getStatusCodeValue()).isEqualTo(201);

        PersonAssignmentRequest person = new PersonAssignmentRequest();
        person.setFullName("   ");
        person.setRole("kontrolor");
        ResponseEntity<String> assigned = restTemplate.postForEntity(
                "/api/companies/" + created.getBody().getId() + "/people",
                new HttpEntity<PersonAssignmentRequest>(person),
                String.class);

        assertThat(assigned.getStatusCodeValue()).isEqualTo(400);
        assertThat(assigned.getBody()).contains("Person full name is required");
    }
}
