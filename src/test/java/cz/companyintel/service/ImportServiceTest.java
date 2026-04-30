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
}
