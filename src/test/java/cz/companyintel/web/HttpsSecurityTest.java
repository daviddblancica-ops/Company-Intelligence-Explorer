package cz.companyintel.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.security.require-https=true")
@AutoConfigureMockMvc
class HttpsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void redirectsInsecureRequestsToHttps() throws Exception {
        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://localhost/api/auth/session"));
    }

    @Test
    void acceptsSecureRequests() throws Exception {
        mockMvc.perform(get("/api/auth/session").secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}
