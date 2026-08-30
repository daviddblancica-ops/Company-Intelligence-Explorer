package cz.companyintel.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ImportLimitsTest {

    @Test
    void rejectsUnsafeConfigurationValues() {
        assertThatThrownBy(() -> new ImportLimits(0, 1000, 100))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-payload-bytes");
        assertThatThrownBy(() -> new ImportLimits(1024, 100001, 100))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-rows");
        assertThatThrownBy(() -> new ImportLimits(1024, 1000, 10001))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-people-per-company");
    }
}
