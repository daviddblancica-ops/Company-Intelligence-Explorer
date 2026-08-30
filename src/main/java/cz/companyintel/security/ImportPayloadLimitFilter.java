package cz.companyintel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.companyintel.config.ImportLimits;
import cz.companyintel.web.ErrorResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ImportPayloadLimitFilter extends OncePerRequestFilter {

    private static final Set<String> IMPORT_BODY_PATHS = new HashSet<String>(Arrays.asList(
            "/api/import/json",
            "/api/import/csv",
            "/api/import/preview/json",
            "/api/import/preview/csv"));

    private final ObjectMapper objectMapper;
    private final int maxPayloadBytes;

    public ImportPayloadLimitFilter(ObjectMapper objectMapper, ImportLimits limits) {
        this.objectMapper = objectMapper;
        this.maxPayloadBytes = limits.getMaxPayloadBytes();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !IMPORT_BODY_PATHS.contains(applicationPath(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (request.getContentLengthLong() > maxPayloadBytes) {
            writePayloadTooLarge(response, request);
            return;
        }

        byte[] body = readBoundedBody(request);
        if (body == null) {
            writePayloadTooLarge(response, request);
            return;
        }
        filterChain.doFilter(new ReplayableBodyRequest(request, body), response);
    }

    private byte[] readBoundedBody(HttpServletRequest request) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxPayloadBytes, 8192));
        byte[] buffer = new byte[8192];
        ServletInputStream input = request.getInputStream();
        int total = 0;
        int read;
        while (true) {
            int remaining = maxPayloadBytes - total;
            if (remaining == 0) {
                return input.read() == -1 ? output.toByteArray() : null;
            }
            read = input.read(buffer, 0, Math.min(buffer.length, remaining));
            if (read == -1) {
                return output.toByteArray();
            }
            total += read;
            output.write(buffer, 0, read);
        }
    }

    private String applicationPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        return contextPath.isEmpty() ? requestUri : requestUri.substring(contextPath.length());
    }

    private void writePayloadTooLarge(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase(),
                "Importní data jsou příliš velká. Povolený limit je " + maxPayloadBytes + " bajtů.",
                request.getRequestURI()));
    }

    private static class ReplayableBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        ReplayableBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Importy jsou zpracovávány synchronně; neblokující čtení se nepoužívá.
                }

                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public int read(byte[] bytes, int offset, int length) {
                    return input.read(bytes, offset, length);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
