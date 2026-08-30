package cz.companyintel.service;

import static org.assertj.core.api.Assertions.assertThat;

import cz.companyintel.domain.ChangeEvent;
import java.io.StringReader;
import java.util.Collections;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

class AuditCsvExporterTest {

    private final AuditCsvExporter exporter = new AuditCsvExporter();

    @Test
    void neutralizesSpreadsheetFormulasAndExportsCompanySnapshot() throws Exception {
        ChangeEvent event = new ChangeEvent(
                "=HYPERLINK(\"https://example.test\",\"firma\")",
                "+23143614",
                "UPDATED",
                "  @SUM(1,2)");

        String csv = exporter.export(Collections.singletonList(event));

        CSVFormat format = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();
        try (CSVParser parser = format.parse(new StringReader(csv.substring(1)))) {
            CSVRecord record = parser.getRecords().get(0);
            assertThat(record.get("subjekt"))
                    .isEqualTo("'=HYPERLINK(\"https://example.test\",\"firma\")");
            assertThat(record.get("IČO")).isEqualTo("'+23143614");
            assertThat(record.get("popis")).isEqualTo("'  @SUM(1,2)");
        }
    }
}
