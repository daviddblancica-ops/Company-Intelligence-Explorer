package cz.companyintel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.companyintel.web.CompanyRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ImportService {

    private final CompanyService companyService;
    private final ObjectMapper objectMapper;

    public ImportService(CompanyService companyService, ObjectMapper objectMapper) {
        this.companyService = companyService;
        this.objectMapper = objectMapper;
    }

    public ImportResult importJson(String body) throws IOException {
        CompanyRequest[] requests = objectMapper.readValue(body, CompanyRequest[].class);
        return saveAll(Arrays.asList(requests));
    }

    public ImportResult importCsv(String body) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(body));
        List<CompanyRequest> requests = new ArrayList<CompanyRequest>();
        String line;
        boolean header = true;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (header) {
                header = false;
                continue;
            }
            String[] columns = line.split(",", -1);
            if (columns.length < 5) {
                continue;
            }
            CompanyRequest request = new CompanyRequest();
            request.setName(columns[0].trim());
            request.setRegistrationNumber(columns[1].trim());
            request.setCountry(columns[2].trim());
            request.setLegalForm(columns[3].trim());
            request.setPeople(parsePeople(columns[4]));
            requests.add(request);
        }
        return saveAll(requests);
    }

    private ImportResult saveAll(List<CompanyRequest> requests) {
        int imported = 0;
        for (CompanyRequest request : requests) {
            companyService.saveCompany(request);
            imported++;
        }
        return new ImportResult(imported);
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
}
