package cz.companyintel.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizationServiceTest {

    private final NormalizationService normalizationService = new NormalizationService();

    @Test
    void normalizesCaseWhitespaceAndDiacritics() {
        assertThat(normalizationService.normalizeName("  Zluta   Firma, s.r.o. "))
                .isEqualTo("zluta firma s r o");
    }
}
