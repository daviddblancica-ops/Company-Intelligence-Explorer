package cz.companyintel.web;

import cz.companyintel.service.AresImportService;
import cz.companyintel.service.ImportPreview;
import cz.companyintel.service.ImportResult;
import cz.companyintel.service.ImportService;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;
    private final AresImportService aresImportService;

    public ImportController(ImportService importService, AresImportService aresImportService) {
        this.importService = importService;
        this.aresImportService = aresImportService;
    }

    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImportResult importJson(@RequestBody String body) throws IOException {
        return importService.importJson(body);
    }

    @PostMapping(value = "/csv", consumes = "text/csv")
    public ImportResult importCsv(@RequestBody String body) throws IOException {
        return importService.importCsv(body);
    }

    @PostMapping(value = "/preview/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImportPreview previewJson(@RequestBody String body) {
        return importService.previewJson(body);
    }

    @PostMapping(value = "/preview/csv", consumes = "text/csv")
    public ImportPreview previewCsv(@RequestBody String body) throws IOException {
        return importService.previewCsv(body);
    }

    @PostMapping("/ares/{ico}")
    public CompanyResponse importAres(@PathVariable String ico) {
        return CompanyResponse.from(aresImportService.importByIco(ico));
    }

    @GetMapping("/runs")
    public List<ImportRunResponse> runs() {
        return importService.findRecentRuns(50).stream()
                .map(ImportRunResponse::from)
                .collect(Collectors.toList());
    }
}
