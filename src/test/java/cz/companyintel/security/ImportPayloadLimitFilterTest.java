package cz.companyintel.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.companyintel.config.ImportLimits;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

class ImportPayloadLimitFilterTest {

    private ImportPayloadLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ImportPayloadLimitFilter(
                new ObjectMapper().findAndRegisterModules(),
                new ImportLimits(32, 100, 10));
    }

    @Test
    void rejectsImportBodyLargerThanConfiguredLimit() throws Exception {
        MockHttpServletRequest request = request("/api/import/json", repeat("x", 33));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("Povolený limit je 32 bajtů");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsOversizedChunkedBodyWithoutContentLength() throws Exception {
        MockHttpServletRequest request = request("/api/import/csv", repeat("x", 33));
        HttpServletRequestWrapper chunked = new HttpServletRequestWrapper(request) {
            @Override
            public int getContentLength() {
                return -1;
            }

            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(chunked, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void replaysAcceptedBodyForMessageConverter() throws Exception {
        String body = "[{\"name\":\"A\"}]";
        MockHttpServletRequest request = request("/api/import/json", body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(StreamUtils.copyToString(
                chain.getRequest().getInputStream(), StandardCharsets.UTF_8)).isEqualTo(body);
    }

    @Test
    void ignoresBodiesOutsideImportEndpoints() throws Exception {
        MockHttpServletRequest request = request("/api/companies", repeat("x", 100));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private MockHttpServletRequest request(String path, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        return request;
    }

    private String repeat(String value, int count) {
        StringBuilder result = new StringBuilder(value.length() * count);
        for (int index = 0; index < count; index++) {
            result.append(value);
        }
        return result.toString();
    }
}
