package cz.companyintel.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.import.max-payload-bytes=128")
@AutoConfigureMockMvc
class ImportPayloadSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsOversizedImportForEditor() throws Exception {
        mockMvc.perform(post("/api/import/json")
                        .with(httpBasic("editor", "editor-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"" + repeat("A", 200) + "\"}]"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.path").value("/api/import/json"));
    }

    @Test
    void rejectsViewerBeforeReadingOversizedImport() throws Exception {
        mockMvc.perform(post("/api/import/json")
                        .with(httpBasic("viewer", "viewer-local-2026"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"name\":\"" + repeat("A", 200) + "\"}]"))
                .andExpect(status().isForbidden());
    }

    private String repeat(String value, int count) {
        return String.join("", Collections.nCopies(count, value));
    }
}
