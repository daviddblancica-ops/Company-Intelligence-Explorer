package cz.companyintel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.import.max-rows=3",
        "app.import.max-people-per-company=2"
})
class ImportServiceTest {

    @Autowired
    private ImportService importService;

    @Autowired
    private CompanyService companyService;

    @Test
    void importsCsvCompanies() throws Exception {
        String csv = "name,registrationNumber,country,legalForm,people\n"
                + "Import Test s.r.o.,11122233,CZ,s.r.o.,Karel Novak|jednatel\n";

        ImportResult result = importService.importCsv(csv);

        assertThat(result.getImported()).isEqualTo(1);
        assertThat(companyService.searchCompanies("import test")).hasSize(1);
    }

    @Test
    void parsesQuotedCommasInCsv() {
        String csv = "name,registrationNumber,country,legalForm,people\n"
                + "\"Quoted, Company s.r.o.\",11122234,CZ,s.r.o.,\"Novak, Karel|jednatel\"\n";

        ImportResult result = importService.importCsv(csv);

        assertThat(result.getImported()).isEqualTo(1);
        assertThat(companyService.searchCompanies("quoted company")).hasSize(1);
    }

    @Test
    void importsJsonCompanies() throws Exception {
        String json = "[{\"name\":\"Json Test a.s.\",\"registrationNumber\":\"44455566\","
                + "\"country\":\"CZ\",\"legalForm\":\"a.s.\",\"people\":[]}]";

        ImportResult result = importService.importJson(json);

        assertThat(result.getImported()).isEqualTo(1);
        assertThat(companyService.searchCompanies("json test")).hasSize(1);
    }

    @Test
    void rejectsEmptyJsonRowsDuringImport() throws Exception {
        ImportResult result = importService.importJson("[null]");

        assertThat(result.getImported()).isZero();
        assertThat(result.getFailed()).isEqualTo(1);
    }

    @Test
    void rejectsMalformedJsonWithoutCreatingImport() {
        assertThatThrownBy(() -> importService.importJson("{broken"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neplatný formát JSON");
    }

    @Test
    void rejectsUnknownJsonFields() {
        String json = "[{\"name\":\"Strict JSON\",\"registrationNumber\":\"10000008\","
                + "\"registrationNubmer\":\"typo\"}]";

        assertThatThrownBy(() -> importService.importJson(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neplatný formát JSON");
    }

    @Test
    void rejectsImportAboveConfiguredRowLimit() {
        String json = "["
                + "{\"name\":\"A\",\"registrationNumber\":\"10000001\"},"
                + "{\"name\":\"B\",\"registrationNumber\":\"10000002\"},"
                + "{\"name\":\"C\",\"registrationNumber\":\"10000003\"},"
                + "{\"name\":\"D\",\"registrationNumber\":\"10000004\"}]";

        assertThatThrownBy(() -> importService.importJson(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nejvýše 3 firem");
    }

    @Test
    void reportsTooManyPeopleAndOversizedFieldsAsInvalidRows() {
        String people = "[{\"fullName\":\"A\",\"role\":\"jednatel\"},"
                + "{\"fullName\":\"B\",\"role\":\"jednatel\"},"
                + "{\"fullName\":\"C\",\"role\":\"jednatel\"}]";
        String json = "[{\"name\":\"People Limit\",\"registrationNumber\":\"10000005\","
                + "\"people\":" + people + "},"
                + "{\"name\":\"" + repeat("X", 256)
                + "\",\"registrationNumber\":\"10000006\"}]";

        ImportResult result = importService.importJson(json);

        assertThat(result.getImported()).isZero();
        assertThat(result.getFailed()).isEqualTo(2);
    }

    @Test
    void rejectsUnexpectedCsvHeader() {
        assertThatThrownBy(() -> importService.importCsv("company,ico,country,form,people\nA,1,CZ,s.r.o.,"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CSV musí obsahovat záhlaví");
    }

    @Test
    void rejectsCsvAboveConfiguredRowLimit() {
        String csv = "name,registrationNumber,country,legalForm,people\n"
                + "A,10000011,CZ,s.r.o.,\n"
                + "B,10000012,CZ,s.r.o.,\n"
                + "C,10000013,CZ,s.r.o.,\n"
                + "D,10000014,CZ,s.r.o.,\n";

        assertThatThrownBy(() -> importService.previewCsv(csv))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nejvýše 3 firem");
    }

    @Test
    void marksMalformedCsvPersonAsRowError() {
        String csv = "name,registrationNumber,country,legalForm,people\n"
                + "Invalid Person s.r.o.,10000007,CZ,s.r.o.,Jméno bez role\n";

        ImportResult result = importService.importCsv(csv);

        assertThat(result.getImported()).isZero();
        assertThat(result.getFailed()).isEqualTo(1);
    }

    @Test
    void previewsCsvRowsBeforeImport() throws Exception {
        String csv = "name,registrationNumber,country,legalForm,people\n"
                + "Preview Test s.r.o.,77788899,CZ,s.r.o.,Jana Validni|jednatelka\n"
                + ",,CZ,s.r.o.,\n"
                + "Broken row\n";

        ImportPreview preview = importService.previewCsv(csv);

        assertThat(preview.getSourceType()).isEqualTo("CSV");
        assertThat(preview.getTotalRows()).isEqualTo(3);
        assertThat(preview.getValidRows()).isEqualTo(1);
        assertThat(preview.getInvalidRows()).isEqualTo(2);
        assertThat(preview.getRows()).extracting(ImportPreviewRow::isValid).containsExactly(true, false, false);
        assertThat(companyService.searchCompanies("preview test")).isEmpty();
    }

    @Test
    void previewsInvalidJsonWithoutSavingImportRun() {
        ImportPreview preview = importService.previewJson("{broken");

        assertThat(preview.getSourceType()).isEqualTo("JSON");
        assertThat(preview.getTotalRows()).isEqualTo(1);
        assertThat(preview.getValidRows()).isZero();
        assertThat(preview.getInvalidRows()).isEqualTo(1);
        assertThat(preview.getRows().get(0).getMessage()).contains("Vstup JSON se nepodařilo zpracovat");
    }

    private String repeat(String value, int count) {
        return String.join("", Collections.nCopies(count, value));
    }
}
