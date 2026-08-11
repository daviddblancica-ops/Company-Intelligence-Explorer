package cz.companyintel.service;

import com.fasterxml.jackson.databind.JsonNode;
import cz.companyintel.domain.Company;
import cz.companyintel.web.CompanyRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class AresImportService {

    private static final String ARES_DETAIL_URL =
            "https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/ekonomicke-subjekty/{ico}";

    private final RestTemplate restTemplate;
    private final CompanyService companyService;

    public AresImportService(RestTemplate restTemplate, CompanyService companyService) {
        this.restTemplate = restTemplate;
        this.companyService = companyService;
    }

    public Company importByIco(String ico) {
        String normalizedIco = normalizeIco(ico);
        JsonNode payload;
        try {
            payload = restTemplate.getForObject(ARES_DETAIL_URL, JsonNode.class, normalizedIco);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException("Subjekt v ARES nebyl nalezen: " + normalizedIco);
        }

        CompanyRequest request = new CompanyRequest();
        request.setName(text(payload, "obchodniJmeno"));
        request.setRegistrationNumber(text(payload, "ico"));
        request.setCountry(text(payload.path("sidlo"), "kodStatu"));
        request.setLegalForm(text(payload, "pravniForma"));
        request.setAddress(text(payload.path("sidlo"), "textovaAdresa"));
        request.setDataSource("ARES");

        return companyService.saveCompany(request);
    }

    private String normalizeIco(String ico) {
        return ico == null ? "" : ico.replaceAll("[^0-9]", "");
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }
}
