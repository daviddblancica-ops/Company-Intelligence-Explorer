package cz.companyintel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.companyintel.domain.ImportRun;
import cz.companyintel.repository.ImportRunRepository;
import cz.companyintel.web.CompanyRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ImportService {

    private final CompanyService companyService;
    private final ObjectMapper objectMapper;
    private final ImportRunRepository importRunRepository;
    private final AuditService auditService;

    public ImportService(
            CompanyService companyService,
            ObjectMapper objectMapper,
            ImportRunRepository importRunRepository,
            AuditService auditService) {
        this.companyService = companyService;
        this.objectMapper = objectMapper;
        this.importRunRepository = importRunRepository;
        this.auditService = auditService;
    }

    public ImportResult importJson(String body) throws IOException {
        try {
            CompanyRequest[] requests = objectMapper.readValue(body, CompanyRequest[].class);
            List<ImportRow> rows = new ArrayList<ImportRow>();
            for (int index = 0; index < requests.length; index++) {
                CompanyRequest request = requests[index];
                rows.add(new ImportRow(index + 1, request, request == null ? "" : request.getRegistrationNumber()));
            }
            return saveAll("JSON", rows);
        } catch (IOException exception) {
            ImportRun run = new ImportRun("JSON");
            run.addError(1, limit(body), "Vstup JSON se nepodařilo zpracovat: " + exception.getMessage());
            run.finish(1, 0, 1);
            ImportRun saved = importRunRepository.save(run);
            auditService.recordImportRun(saved);
            throw exception;
        }
    }

    public ImportResult importCsv(String body) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(body));
        ImportRun run = new ImportRun("CSV");
        List<ImportRow> rows = new ArrayList<ImportRow>();
        String line;
        boolean header = true;
        int rowNumber = 0;
        while ((line = reader.readLine()) != null) {
            rowNumber++;
            if (line.trim().isEmpty()) {
                continue;
            }
            if (header) {
                header = false;
                continue;
            }
            String[] columns = line.split(",", -1);
            if (columns.length < 5) {
                run.addError(rowNumber, limit(line), "Očekáváno 5 sloupců CSV, nalezeno " + columns.length);
                continue;
            }
            CompanyRequest request = new CompanyRequest();
            request.setName(columns[0].trim());
            request.setRegistrationNumber(columns[1].trim());
            request.setCountry(columns[2].trim());
            request.setLegalForm(columns[3].trim());
            request.setPeople(parsePeople(columns[4]));
            rows.add(new ImportRow(rowNumber, request, line));
        }
        return saveAll(run, rows);
    }

    public ImportPreview previewJson(String body) {
        try {
            CompanyRequest[] requests = objectMapper.readValue(body, CompanyRequest[].class);
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

    public ImportPreview previewCsv(String body) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(body));
        List<ImportPreviewRow> previewRows = new ArrayList<ImportPreviewRow>();
        String line;
        boolean header = true;
        int rowNumber = 0;
        int valid = 0;
        int invalid = 0;
        while ((line = reader.readLine()) != null) {
            rowNumber++;
            if (line.trim().isEmpty()) {
                continue;
            }
            if (header) {
                header = false;
                continue;
            }
            String[] columns = line.split(",", -1);
            if (columns.length < 5) {
                invalid++;
                previewRows.add(new ImportPreviewRow(
                        rowNumber,
                        false,
                        "",
                        "",
                        "Očekáváno 5 sloupců CSV, nalezeno " + columns.length,
                        limit(line)));
                continue;
            }
            CompanyRequest request = new CompanyRequest();
            request.setName(columns[0].trim());
            request.setRegistrationNumber(columns[1].trim());
            request.setCountry(columns[2].trim());
            request.setLegalForm(columns[3].trim());
            request.setPeople(parsePeople(columns[4]));
            String validationError = validate(request);
            boolean rowValid = validationError == null;
            if (rowValid) {
                valid++;
            } else {
                invalid++;
            }
            previewRows.add(toPreviewRow(rowNumber, request, rowValid, validationError, line));
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
        for (String entry : entries) {
            String[] parts = entry.split("\\|", -1);
            if (parts.length == 2) {
                CompanyRequest.PersonRole role = new CompanyRequest.PersonRole();
                role.setFullName(parts[0].trim());
                role.setRole(parts[1].trim());
                people.add(role);
            }
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
        return null;
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
}
