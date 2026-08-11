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
        assertThat(production.getProperty("server.forward-headers-strategy")).isEqualTo("native");
        assertThat(production.getProperty("server.servlet.session.cookie.secure"))
                .isEqualTo("${SESSION_COOKIE_SECURE:true}");
        assertThat(production.getProperty("app.security.require-https"))
                .isEqualTo("${REQUIRE_HTTPS:true}");
        assertThat(production.getProperty("app.integrations.ares.connect-timeout"))
                .isEqualTo("${ARES_CONNECT_TIMEOUT:3s}");
        assertThat(production.getProperty("app.integrations.ares.read-timeout"))
                .isEqualTo("${ARES_READ_TIMEOUT:10s}");
        assertThat(production.getProperty("app.security.rate-limit.enabled"))
                .isEqualTo("${RATE_LIMIT_ENABLED:true}");
        assertThat(production.getProperty("app.security.rate-limit.login.max-requests"))
                .isEqualTo("${RATE_LIMIT_LOGIN_MAX_REQUESTS:10}");
        assertThat(production.getProperty("app.security.rate-limit.ares.max-requests"))
                .isEqualTo("${RATE_LIMIT_ARES_MAX_REQUESTS:30}");
        assertThat(production.getProperty("app.security.users.admin.username"))
                .isEqualTo("${APP_ADMIN_USERNAME}");
        assertThat(production.getProperty("app.security.users.admin.password"))
                .isEqualTo("${APP_ADMIN_PASSWORD}");
        assertThat(production.getProperty("app.security.users.editor.password"))
                .isEqualTo("${APP_EDITOR_PASSWORD}");
        assertThat(production.getProperty("app.security.users.viewer.password"))
                .isEqualTo("${APP_VIEWER_PASSWORD}");
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
