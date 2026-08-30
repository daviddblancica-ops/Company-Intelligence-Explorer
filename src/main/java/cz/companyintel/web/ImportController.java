package cz.companyintel.web;

import cz.companyintel.service.AresImportService;
import cz.companyintel.service.ImportPreview;
import cz.companyintel.service.ImportResult;
import cz.companyintel.service.ImportService;
import cz.companyintel.security.AuthorizationRules;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/import")
@PreAuthorize(AuthorizationRules.READ)
public class ImportController {

    private final ImportService importService;
    private final AresImportService aresImportService;

    public ImportController(ImportService importService, AresImportService aresImportService) {
        this.importService = importService;
        this.aresImportService = aresImportService;
    }

    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AuthorizationRules.EDIT)
    public ImportResult importJson(@RequestBody String body) {
        return importService.importJson(body);
    }

    @PostMapping(value = "/csv", consumes = "text/csv")
    @PreAuthorize(AuthorizationRules.EDIT)
    public ImportResult importCsv(@RequestBody String body) {
        return importService.importCsv(body);
    }

    @PostMapping(value = "/preview/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AuthorizationRules.EDIT)
    public ImportPreview previewJson(@RequestBody String body) {
        return importService.previewJson(body);
    }

    @PostMapping(value = "/preview/csv", consumes = "text/csv")
    @PreAuthorize(AuthorizationRules.EDIT)
    public ImportPreview previewCsv(@RequestBody String body) {
        return importService.previewCsv(body);
    }

    @PostMapping("/ares/{ico}")
    @PreAuthorize(AuthorizationRules.EDIT)
    public CompanyResponse importAres(@PathVariable String ico) {
        return CompanyResponse.from(aresImportService.importByIco(ico));
    }

    @GetMapping("/runs")
    public List<ImportRunResponse> runs() {
        return importService.findRecentRuns(50).stream()
                .map(ImportRunResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/runs/{id}")
    public ImportRunResponse run(@PathVariable Long id) {
        return ImportRunResponse.from(importService.findRun(id));
    }
}
