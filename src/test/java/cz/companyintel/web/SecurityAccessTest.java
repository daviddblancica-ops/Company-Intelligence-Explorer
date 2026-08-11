package cz.companyintel.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesHomeHealthAndAnonymousSessionWithoutLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void rejectsProtectedApiForAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/companies/search"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Pro tuto operaci se musíte přihlásit."));
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
        mockMvc.perform(post("/api/tasks")
                        .with(httpBasic("viewer", "viewer-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Zakázaná změna\",\"segment\":\"Projekt\",\"priority\":\"LOW\"}"))
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
}
