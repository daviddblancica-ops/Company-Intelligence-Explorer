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

    public ImportService(CompanyService companyService, ObjectMapper objectMapper, ImportRunRepository importRunRepository) {
        this.companyService = companyService;
        this.objectMapper = objectMapper;
        this.importRunRepository = importRunRepository;
    }

    public ImportResult importJson(String body) throws IOException {
        try {
            CompanyRequest[] requests = objectMapper.readValue(body, CompanyRequest[].class);
            List<ImportRow> rows = new ArrayList<ImportRow>();
            for (int index = 0; index < requests.length; index++) {
                rows.add(new ImportRow(index + 1, requests[index], requests[index].getRegistrationNumber()));
            }
            return saveAll("JSON", rows);
        } catch (IOException exception) {
            ImportRun run = new ImportRun("JSON");
            run.addError(1, limit(body), "JSON input could not be parsed: " + exception.getMessage());
            run.finish(1, 0, 1);
            importRunRepository.save(run);
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
                run.addError(rowNumber, limit(line), "Expected 5 CSV columns but found " + columns.length);
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

    public List<ImportRun> findRecentRuns(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return importRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, safeLimit));
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
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return "Company name is required";
        }
        if (request.getRegistrationNumber() == null || request.getRegistrationNumber().trim().isEmpty()) {
            return "Registration number is required";
        }
        return null;
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
