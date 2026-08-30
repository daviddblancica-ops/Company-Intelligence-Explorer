package cz.companyintel.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cz.companyintel.domain.Company;
import cz.companyintel.service.CompanyService;
import cz.companyintel.service.PersonService;
import java.time.LocalDate;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PersonService personService;

    @Test
    void exposesHomeAndAnonymousSessionWithoutLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void rejectsProtectedApiForAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/companies/search"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Pro tuto operaci se musíte přihlásit."));
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsAuthenticatedSessionThroughLoginEndpoint() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", "viewer")
                        .param("password", "viewer-local-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("viewer"))
                .andExpect(jsonPath("$.roles[0]").value("VIEWER"))
                .andReturn();

        HttpSession servletSession = login.getRequest().getSession(false);
        MockHttpSession session = (MockHttpSession) servletSession;
        mockMvc.perform(get("/api/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.canEdit").value(false));
    }

    @Test
    void rejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "spatne-heslo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Neplatné uživatelské jméno nebo heslo."));
    }

    @Test
    void viewerCanReadButCannotChangeRecords() throws Exception {
        mockMvc.perform(get("/api/companies/search")
                        .with(httpBasic("viewer", "viewer-local-2026")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/health")
                        .with(httpBasic("viewer", "viewer-local-2026")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks")
                        .with(httpBasic("viewer", "viewer-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Zakázaná změna\",\"segment\":\"Projekt\",\"priority\":\"LOW\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotBypassAuthorizationByChangingRecordIds() throws Exception {
        String unknownId = "9223372036854775807";

        mockMvc.perform(put("/api/companies/" + unknownId)
                        .with(httpBasic("viewer", "viewer-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/companies/" + unknownId + "/watchlist")
                        .with(httpBasic("viewer", "viewer-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"watchlisted\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/people/" + unknownId)
                        .with(httpBasic("viewer", "viewer-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/companies/" + unknownId + "/people/" + unknownId)
                        .with(httpBasic("viewer", "viewer-local-2026"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void editorCanChangeDataButCannotDeleteWholeCompanyOrArchiveAudit() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(httpBasic("editor", "editor-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Povolená změna\",\"segment\":\"Projekt\",\"priority\":\"LOW\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/companies/999999")
                        .with(httpBasic("editor", "editor-local-2026"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/audit/archive")
                        .with(httpBasic("editor", "editor-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[],\"archived\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndDeleteCompany() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/companies")
                        .with(httpBasic("admin", "admin-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Security Test s.r.o.\",\"registrationNumber\":\"87654321\",\"country\":\"CZ\",\"legalForm\":\"s.r.o.\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String locationBody = created.getResponse().getContentAsString();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"id\":(\\d+)").matcher(locationBody);
        org.assertj.core.api.Assertions.assertThat(matcher.find()).isTrue();

        mockMvc.perform(delete("/api/companies/" + matcher.group(1))
                        .with(httpBasic("admin", "admin-local-2026"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void viewerCannotReadSensitivePersonDetails() throws Exception {
        CompanyRequest companyRequest = new CompanyRequest();
        companyRequest.setName("Oprávnění osob s.r.o.");
        companyRequest.setRegistrationNumber("11223344");
        companyRequest.setCountry("CZ");
        companyRequest.setLegalForm("s.r.o.");
        Company company = companyService.saveCompany(companyRequest);
        Company assigned = companyService.assignPerson(company.getId(), "Citlivá Osoba", "jednatel");
        Long personId = assigned.getPeople().iterator().next().getPerson().getId();

        PersonUpdateRequest personRequest = new PersonUpdateRequest();
        personRequest.setFullName("Citlivá Osoba");
        personRequest.setDateOfBirth(LocalDate.of(1985, 6, 15));
        personRequest.setResidenceAddress("Soukromá 12, Praha");
        personRequest.setNote("Interní poznámka");
        personService.updatePerson(personId, personRequest);

        mockMvc.perform(get("/api/people/" + personId)
                        .with(httpBasic("viewer", "viewer-local-2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensitiveDetailsVisible").value(false))
                .andExpect(jsonPath("$.dateOfBirth").doesNotExist())
                .andExpect(jsonPath("$.residenceAddress").doesNotExist())
                .andExpect(jsonPath("$.note").doesNotExist());

        mockMvc.perform(get("/api/people")
                        .param("q", "Citlivá")
                        .with(httpBasic("viewer", "viewer-local-2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensitiveDetailsVisible").value(false))
                .andExpect(jsonPath("$[0].dateOfBirth").doesNotExist())
                .andExpect(jsonPath("$[0].residenceAddress").doesNotExist())
                .andExpect(jsonPath("$[0].note").doesNotExist());

        mockMvc.perform(get("/api/people/" + personId)
                        .with(httpBasic("editor", "editor-local-2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensitiveDetailsVisible").value(true))
                .andExpect(jsonPath("$.dateOfBirth").value("1985-06-15"))
                .andExpect(jsonPath("$.residenceAddress").value("Soukromá 12, Praha"))
                .andExpect(jsonPath("$.note").value("Interní poznámka"));
    }
}
