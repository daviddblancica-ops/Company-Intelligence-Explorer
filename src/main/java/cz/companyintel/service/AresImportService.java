package cz.companyintel.service;

import com.fasterxml.jackson.databind.JsonNode;
import cz.companyintel.domain.Company;
import cz.companyintel.web.CompanyRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class AresImportService {

    private static final String ARES_DETAIL_URL =
            "https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/ekonomicke-subjekty/{ico}";
    private static final String ARES_PUBLIC_REGISTER_URL =
            "https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/ekonomicke-subjekty-vr/{ico}";

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
        request.setIncorporationDate(date(payload, "datumVzniku"));

        JsonNode registerPayload = publicRegister(normalizedIco);
        JsonNode registerRecord = primaryRecord(registerPayload);
        request.setRegistryFileNumber(registryFileNumber(payload, registerRecord));
        request.setRegistryRegistrationDate(date(registerRecord, "datumZapisu"));

        JsonNode capital = currentValue(registerRecord == null ? null : registerRecord.path("zakladniKapital"));
        JsonNode contribution = capital == null ? null : capital.path("vklad");
        request.setShareCapital(decimal(contribution, "hodnota"));
        request.setShareCapitalCurrency(currency(text(contribution, "typObnos")));

        return companyService.saveCompany(request);
    }

    private JsonNode publicRegister(String ico) {
        try {
            return restTemplate.getForObject(ARES_PUBLIC_REGISTER_URL, JsonNode.class, ico);
        } catch (HttpClientErrorException.NotFound exception) {
            return null;
        }
    }

    private JsonNode primaryRecord(JsonNode payload) {
        JsonNode records = payload == null ? null : payload.path("zaznamy");
        if (records == null || !records.isArray() || records.size() == 0) {
            return null;
        }
        for (JsonNode record : records) {
            if (record.path("primarniZaznam").asBoolean(false)) {
                return record;
            }
        }
        return records.get(0);
    }

    private String registryFileNumber(JsonNode subject, JsonNode registerRecord) {
        JsonNode fileNumber = currentValue(registerRecord == null ? null : registerRecord.path("spisovaZnacka"));
        String section = text(fileNumber, "oddil");
        String insert = text(fileNumber, "vlozka");
        String court = text(fileNumber, "soud");
        if (!section.isEmpty() && !insert.isEmpty()) {
            return section + " " + insert + (court.isEmpty() ? "" : "/" + court);
        }

        JsonNode otherData = subject == null ? null : subject.path("dalsiUdaje");
        if (otherData != null && otherData.isArray()) {
            for (JsonNode source : otherData) {
                if ("vr".equalsIgnoreCase(text(source, "datovyZdroj"))) {
                    return text(source, "spisovaZnacka");
                }
            }
        }
        return "";
    }

    private JsonNode currentValue(JsonNode values) {
        if (values == null || !values.isArray() || values.size() == 0) {
            return null;
        }
        JsonNode current = null;
        for (JsonNode value : values) {
            if (!value.hasNonNull("datumVymazu")) {
                current = value;
            }
        }
        return current;
    }

    private LocalDate date(JsonNode node, String field) {
        String value = text(node, field);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field).replace(" ", "").replace(',', '.');
        if (value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String currency(String aresType) {
        if ("KORUNY".equalsIgnoreCase(aresType)) {
            return "CZK";
        }
        if ("EURA".equalsIgnoreCase(aresType)) {
            return "EUR";
        }
        if ("DOLARY".equalsIgnoreCase(aresType)) {
            return "USD";
        }
        return aresType;
    }

    private String normalizeIco(String ico) {
        return ico == null ? "" : ico.replaceAll("[^0-9]", "");
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }
}
