package cz.companyintel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.companyintel.domain.Company;
import cz.companyintel.web.CompanyRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
class AresImportServiceTest {

    private static final String DETAIL_URL =
            "https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/ekonomicke-subjekty/{ico}";
    private static final String REGISTER_URL =
            "https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/ekonomicke-subjekty-vr/{ico}";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CompanyService companyService;

    private AresImportService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        service = new AresImportService(restTemplate, companyService);
        objectMapper = new ObjectMapper();
    }

    @Test
    void importsRegistryDatesFileNumberAndShareCapital() throws Exception {
        JsonNode detail = objectMapper.readTree("{"
                + "\"ico\":\"23143614\","
                + "\"obchodniJmeno\":\"David Kozák International s.r.o.\","
                + "\"pravniForma\":\"112\","
                + "\"datumVzniku\":\"2025-04-03\","
                + "\"sidlo\":{\"kodStatu\":\"CZ\",\"textovaAdresa\":\"Ústí nad Labem\"}"
                + "}");
        JsonNode register = objectMapper.readTree("{\"zaznamy\":[{"
                + "\"primarniZaznam\":true,\"datumZapisu\":\"2025-04-03\","
                + "\"spisovaZnacka\":[{\"soud\":\"KSUL\",\"oddil\":\"C\",\"vlozka\":53832}],"
                + "\"zakladniKapital\":[{\"vklad\":{\"typObnos\":\"KORUNY\",\"hodnota\":\"3000\"}}]"
                + "}]}");
        Company saved = new Company("David Kozák International s.r.o.", "david kozak international s r o",
                "23143614", "CZ", "112");

        when(restTemplate.getForObject(DETAIL_URL, JsonNode.class, "23143614")).thenReturn(detail);
        when(restTemplate.getForObject(REGISTER_URL, JsonNode.class, "23143614")).thenReturn(register);
        when(companyService.saveCompany(org.mockito.ArgumentMatchers.any(CompanyRequest.class))).thenReturn(saved);

        service.importByIco("231 436 14");

        ArgumentCaptor<CompanyRequest> request = ArgumentCaptor.forClass(CompanyRequest.class);
        verify(companyService).saveCompany(request.capture());
        assertThat(request.getValue().getRegistryFileNumber()).isEqualTo("C 53832/KSUL");
        assertThat(request.getValue().getRegistryRegistrationDate()).isEqualTo(LocalDate.of(2025, 4, 3));
        assertThat(request.getValue().getIncorporationDate()).isEqualTo(LocalDate.of(2025, 4, 3));
        assertThat(request.getValue().getShareCapital()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(request.getValue().getShareCapitalCurrency()).isEqualTo("CZK");
    }

    @Test
    void rejectsInvalidIcoBeforeCallingAres() {
        assertThatThrownBy(() -> service.importByIco("123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("IČO musí obsahovat přesně 8 číslic");

        verifyNoInteractions(restTemplate, companyService);
    }

    @Test
    void reportsAresTimeoutAsUnavailableService() {
        when(restTemplate.getForObject(DETAIL_URL, JsonNode.class, "23143614"))
                .thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> service.importByIco("23143614"))
                .isInstanceOf(ExternalServiceUnavailableException.class)
                .hasMessage("ARES nyní neodpovídá. Zkuste import zopakovat později.");

        verifyNoInteractions(companyService);
    }
}
