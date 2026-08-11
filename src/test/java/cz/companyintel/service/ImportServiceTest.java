package cz.companyintel.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
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
}
