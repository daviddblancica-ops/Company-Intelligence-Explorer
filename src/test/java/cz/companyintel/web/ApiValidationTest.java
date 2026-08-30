package cz.companyintel.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsMissingRequiredCompanyFieldsBeforeDatabaseAccess() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .with(httpBasic("editor", "editor-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"registrationNumber\":\"23143614\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Název firmy je povinný"));
    }

    @Test
    void validatesNestedPeopleInCompanyPayload() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .with(httpBasic("editor", "editor-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Validace s.r.o.\",\"registrationNumber\":\"23143614\","
                                + "\"people\":[{\"fullName\":\"Jan Novák\",\"role\":\"   \"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Role osoby je povinná"));
    }

    @Test
    void rejectsEmptyAuditBulkOperation() throws Exception {
        mockMvc.perform(post("/api/audit/archive")
                        .with(httpBasic("admin", "admin-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[],\"archived\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Je vyžadováno alespoň jedno ID auditní události"));
    }

    @Test
    void reportsMalformedJsonAsClientError() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(httpBasic("editor", "editor-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tělo požadavku není platný JSON"));
    }

    @Test
    void reportsInvalidQueryParameterFormatAsClientError() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .param("from", "neplatne-datum")
                        .with(httpBasic("viewer", "viewer-local-2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametr from má neplatný formát"));
    }
}
