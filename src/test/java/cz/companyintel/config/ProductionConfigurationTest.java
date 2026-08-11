package cz.companyintel.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ProductionConfigurationTest {

    @Test
    void productionUsesVersionedMigrationsAndSchemaValidation() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load(
                "production",
                new ClassPathResource("application-prod.yml"));

        assertThat(sources).hasSize(1);
        PropertySource<?> production = sources.get(0);
        assertThat(production.getProperty("spring.flyway.enabled")).isEqualTo(true);
        assertThat(production.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo(true);
        assertThat(production.getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration/mariadb");
        assertThat(production.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }

    @Test
    void mariaDbMigrationChainIsPackaged() {
        assertThat(new ClassPathResource(
                "db/migration/mariadb/V1__baseline_schema.sql").exists()).isTrue();
        assertThat(new ClassPathResource(
                "db/migration/mariadb/V2__link_import_runs_to_audit.sql").exists()).isTrue();
        assertThat(new ClassPathResource(
                "db/migration/mariadb/V3__add_audit_severity.sql").exists()).isTrue();
        assertThat(new ClassPathResource(
                "db/migration/mariadb/V4__preserve_audit_subjects_and_extend_people.sql").exists()).isTrue();
        assertThat(new ClassPathResource(
                "db/migration/mariadb/V5__add_czech_diacritics_to_default_tasks.sql").exists()).isTrue();
    }
}
