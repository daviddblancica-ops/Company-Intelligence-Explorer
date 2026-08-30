package cz.companyintel.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import cz.companyintel.config.ImportLimits;
import cz.companyintel.domain.ImportRun;
import cz.companyintel.repository.ImportRunRepository;
import cz.companyintel.web.CompanyRequest;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ImportService {

    private static final List<String> CSV_HEADERS = Arrays.asList(
            "name", "registrationNumber", "country", "legalForm", "people");
    private static final CSVFormat CSV_FORMAT = CSVFormat.RFC4180.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setIgnoreSurroundingSpaces(true)
            .setTrim(true)
            .get();

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_REGISTRATION_NUMBER_LENGTH = 64;
    private static final int MAX_COUNTRY_LENGTH = 3;
    private static final int MAX_LEGAL_FORM_LENGTH = 255;
    private static final int MAX_ADDRESS_LENGTH = 600;
    private static final int MAX_DATA_SOURCE_LENGTH = 255;
    private static final int MAX_REGISTRY_FILE_NUMBER_LENGTH = 120;
    private static final int MAX_CURRENCY_LENGTH = 12;
    private static final int MAX_ROLE_LENGTH = 255;

    private final CompanyService companyService;
    private final ObjectReader companyRequestReader;
    private final ImportRunRepository importRunRepository;
    private final AuditService auditService;
    private final ImportLimits limits;

    public ImportService(
            CompanyService companyService,
            ObjectMapper objectMapper,
            ImportRunRepository importRunRepository,
            AuditService auditService,
            ImportLimits limits) {
        this.companyService = companyService;
        this.companyRequestReader = objectMapper.readerFor(CompanyRequest[].class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.importRunRepository = importRunRepository;
        this.auditService = auditService;
        this.limits = limits;
    }

    public ImportResult importJson(String body) {
        try {
            CompanyRequest[] requests = parseJson(body);
            ensureRowLimit(requests.length);
            List<ImportRow> rows = new ArrayList<ImportRow>();
            for (int index = 0; index < requests.length; index++) {
                CompanyRequest request = requests[index];
                rows.add(new ImportRow(index + 1, request, request == null ? "" : request.getRegistrationNumber()));
            }
            return saveAll("JSON", rows);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Vstup JSON se nepodařilo zpracovat: neplatný formát JSON.");
        }
    }

    public ImportResult importCsv(String body) {
        ImportRun run = new ImportRun("CSV");
        List<ImportRow> rows = new ArrayList<ImportRow>();
        for (ParsedCsvRow row : parseCsv(body)) {
            if (row.error != null) {
                run.addError(row.rowNumber, limit(row.rawValue), row.error);
                continue;
            }
            rows.add(new ImportRow(row.rowNumber, row.request, row.rawValue));
        }
        return saveAll(run, rows);
    }

    public ImportPreview previewJson(String body) {
        try {
            CompanyRequest[] requests = parseJson(body);
            ensureRowLimit(requests.length);
            List<ImportPreviewRow> previewRows = new ArrayList<ImportPreviewRow>();
            int valid = 0;
            int invalid = 0;
            for (int index = 0; index < requests.length; index++) {
                CompanyRequest request = requests[index];
                String validationError = validate(request);
                boolean rowValid = validationError == null;
                if (rowValid) {
                    valid++;
                } else {
                    invalid++;
                }
                previewRows.add(toPreviewRow(index + 1, request, rowValid, validationError, request == null ? "" : request.getRegistrationNumber()));
            }
            return new ImportPreview("JSON", previewRows.size(), valid, invalid, previewRows);
        } catch (IOException exception) {
            List<ImportPreviewRow> rows = new ArrayList<ImportPreviewRow>();
            rows.add(new ImportPreviewRow(1, false, "", "", "Vstup JSON se nepodařilo zpracovat: "
                    + exception.getMessage(), limit(body)));
            return new ImportPreview("JSON", 1, 0, 1, rows);
        }
    }

    public ImportPreview previewCsv(String body) {
        List<ImportPreviewRow> previewRows = new ArrayList<ImportPreviewRow>();
        int valid = 0;
        int invalid = 0;
        for (ParsedCsvRow row : parseCsv(body)) {
            if (row.error != null) {
                invalid++;
                previewRows.add(new ImportPreviewRow(
                        row.rowNumber,
                        false,
                        "",
                        "",
                        row.error,
                        limit(row.rawValue)));
                continue;
            }
            String validationError = validate(row.request);
            boolean rowValid = validationError == null;
            if (rowValid) {
                valid++;
            } else {
                invalid++;
            }
            previewRows.add(toPreviewRow(
                    row.rowNumber, row.request, rowValid, validationError, row.rawValue));
        }
        return new ImportPreview("CSV", previewRows.size(), valid, invalid, previewRows);
    }

    public List<ImportRun> findRecentRuns(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return importRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, safeLimit));
    }

    public ImportRun findRun(Long id) {
        return importRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Importní běh nebyl nalezen: " + id));
    }

    private ImportResult saveAll(String sourceType, List<ImportRow> rows) {
        return saveAll(new ImportRun(sourceType), rows);
    }

    private ImportResult saveAll(ImportRun run, List<ImportRow> rows) {
        int imported = 0;
        int initialFailed = run.getErrors().size();
        int failed = initialFailed;
        for (ImportRow row : rows) {
            String validationError = validate(row.request);
            if (validationError != null) {
                run.addError(row.rowNumber, limit(row.rawValue), validationError);
                failed++;
                continue;
            }
            try {
                companyService.saveCompany(row.request);
                imported++;
            } catch (RuntimeException exception) {
                run.addError(row.rowNumber, limit(row.rawValue), exception.getMessage());
                failed++;
            }
        }
        run.finish(rows.size() + initialFailed, imported, failed);
        ImportRun saved = importRunRepository.save(run);
        auditService.recordImportRun(saved);
        return new ImportResult(saved.getImportedRows(), saved.getFailedRows(), saved.getId(), saved.getStatus());
    }

    private List<CompanyRequest.PersonRole> parsePeople(String value) {
        List<CompanyRequest.PersonRole> people = new ArrayList<CompanyRequest.PersonRole>();
        if (value == null || value.trim().isEmpty()) {
            return people;
        }
        String[] entries = value.split(";");
        if (entries.length > limits.getMaxPeoplePerCompany()) {
            throw new IllegalArgumentException("Firma může obsahovat nejvýše "
                    + limits.getMaxPeoplePerCompany() + " osob.");
        }
        for (String entry : entries) {
            String[] parts = entry.split("\\|", -1);
            if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Osoby v CSV musí používat formát Jméno|role oddělený středníkem.");
            }
            CompanyRequest.PersonRole role = new CompanyRequest.PersonRole();
            role.setFullName(parts[0].trim());
            role.setRole(parts[1].trim());
            people.add(role);
        }
        return people;
    }

    private String validate(CompanyRequest request) {
        if (request == null) {
            return "Řádek firmy je prázdný";
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return "Název firmy je povinný";
        }
        if (request.getRegistrationNumber() == null || request.getRegistrationNumber().trim().isEmpty()) {
            return "IČO je povinné";
        }
        String error = validateText(request.getName(), "Název firmy", MAX_NAME_LENGTH);
        if (error != null) {
            return error;
        }
        error = validateText(
                request.getRegistrationNumber(), "Identifikace firmy", MAX_REGISTRATION_NUMBER_LENGTH);
        if (error != null) {
            return error;
        }
        error = validateText(request.getCountry(), "Kód země", MAX_COUNTRY_LENGTH);
        if (error != null) {
            return error;
        }
        error = validateText(request.getLegalForm(), "Právní forma", MAX_LEGAL_FORM_LENGTH);
        if (error != null) {
            return error;
        }
        error = validateText(request.getAddress(), "Adresa", MAX_ADDRESS_LENGTH);
        if (error != null) {
            return error;
        }
        error = validateText(request.getDataSource(), "Zdroj dat", MAX_DATA_SOURCE_LENGTH);
        if (error != null) {
            return error;
        }
        error = validateText(
                request.getRegistryFileNumber(), "Spisová značka", MAX_REGISTRY_FILE_NUMBER_LENGTH);
        if (error != null) {
            return error;
        }
        error = validateText(request.getShareCapitalCurrency(), "Měna kapitálu", MAX_CURRENCY_LENGTH);
        if (error != null) {
            return error;
        }
        error = validateCapital(request.getShareCapital());
        if (error != null) {
            return error;
        }
        List<CompanyRequest.PersonRole> people = request.getPeople();
        if (people != null && people.size() > limits.getMaxPeoplePerCompany()) {
            return "Firma může obsahovat nejvýše " + limits.getMaxPeoplePerCompany() + " osob";
        }
        if (people != null) {
            for (CompanyRequest.PersonRole person : people) {
                if (person == null || person.getFullName() == null || person.getFullName().trim().isEmpty()) {
                    return "Jméno osoby je povinné";
                }
                if (person.getRole() == null || person.getRole().trim().isEmpty()) {
                    return "Role osoby je povinná";
                }
                error = validateText(person.getFullName(), "Jméno osoby", MAX_NAME_LENGTH);
                if (error != null) {
                    return error;
                }
                error = validateText(person.getRole(), "Role osoby", MAX_ROLE_LENGTH);
                if (error != null) {
                    return error;
                }
            }
        }
        return null;
    }

    private List<ParsedCsvRow> parseCsv(String body) {
        String input = stripBom(body == null ? "" : body);
        try (CSVParser parser = CSV_FORMAT.parse(new StringReader(input))) {
            validateCsvHeader(parser.getHeaderMap());
            List<ParsedCsvRow> rows = new ArrayList<ParsedCsvRow>();
            int rowNumber = 1;
            for (CSVRecord record : parser) {
                rowNumber++;
                ensureRowLimit(rows.size() + 1);
                String rawValue = rawValue(record);
                if (record.size() != CSV_HEADERS.size()) {
                    rows.add(new ParsedCsvRow(
                            rowNumber,
                            null,
                            rawValue,
                            "Očekáváno 5 sloupců CSV, nalezeno " + record.size()));
                    continue;
                }
                try {
                    CompanyRequest request = new CompanyRequest();
                    request.setName(record.get("name"));
                    request.setRegistrationNumber(record.get("registrationNumber"));
                    request.setCountry(record.get("country"));
                    request.setLegalForm(record.get("legalForm"));
                    request.setPeople(parsePeople(record.get("people")));
                    rows.add(new ParsedCsvRow(rowNumber, request, rawValue, null));
                } catch (IllegalArgumentException exception) {
                    rows.add(new ParsedCsvRow(rowNumber, null, rawValue, exception.getMessage()));
                }
            }
            return rows;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Vstup CSV se nepodařilo zpracovat: neplatný formát CSV.");
        }
    }

    private CompanyRequest[] parseJson(String body) throws IOException {
        CompanyRequest[] requests = companyRequestReader.readValue(body);
        if (requests == null) {
            throw new IllegalArgumentException("Vstup JSON musí obsahovat pole firem.");
        }
        return requests;
    }

    private void validateCsvHeader(Map<String, Integer> headerMap) {
        List<String> actual = new ArrayList<String>();
        for (String header : headerMap.keySet()) {
            actual.add(header == null ? "" : header.trim());
        }
        if (!CSV_HEADERS.equals(actual)) {
            throw new IllegalArgumentException(
                    "CSV musí obsahovat záhlaví: name,registrationNumber,country,legalForm,people");
        }
    }

    private void ensureRowLimit(int rowCount) {
        if (rowCount > limits.getMaxRows()) {
            throw new IllegalArgumentException(
                    "Jeden import může obsahovat nejvýše " + limits.getMaxRows() + " firem.");
        }
    }

    private String validateText(String value, String field, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.trim().length() > maxLength) {
            return field + " může obsahovat nejvýše " + maxLength + " znaků";
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                return field + " obsahuje nepovolený řídicí znak";
            }
        }
        return null;
    }

    private String validateCapital(BigDecimal capital) {
        if (capital == null) {
            return null;
        }
        if (capital.signum() < 0) {
            return "Základní kapitál nesmí být záporný";
        }
        BigDecimal normalized = capital.stripTrailingZeros();
        int decimalPlaces = Math.max(normalized.scale(), 0);
        int integerDigits = normalized.precision() - normalized.scale();
        if (integerDigits > 17 || decimalPlaces > 2) {
            return "Základní kapitál může mít nejvýše 17 číslic před desetinnou čárkou"
                    + " a 2 desetinná místa";
        }
        return null;
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private String rawValue(CSVRecord record) {
        StringBuilder value = new StringBuilder();
        for (String column : record) {
            if (value.length() > 0) {
                value.append(',');
            }
            value.append(column);
        }
        return value.toString();
    }

    private ImportPreviewRow toPreviewRow(int rowNumber, CompanyRequest request, boolean valid, String message, String rawValue) {
        return new ImportPreviewRow(
                rowNumber,
                valid,
                request == null ? "" : request.getName(),
                request == null ? "" : request.getRegistrationNumber(),
                valid ? "Připraveno k importu" : message,
                limit(rawValue));
    }

    private String limit(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 1200 ? value.substring(0, 1200) : value;
    }

    private static class ImportRow {
        private final int rowNumber;
        private final CompanyRequest request;
        private final String rawValue;

        private ImportRow(int rowNumber, CompanyRequest request, String rawValue) {
            this.rowNumber = rowNumber;
            this.request = request;
            this.rawValue = rawValue;
        }
    }

    private static class ParsedCsvRow {
        private final int rowNumber;
        private final CompanyRequest request;
        private final String rawValue;
        private final String error;

        private ParsedCsvRow(int rowNumber, CompanyRequest request, String rawValue, String error) {
            this.rowNumber = rowNumber;
            this.request = request;
            this.rawValue = rawValue;
            this.error = error;
        }
    }
}
