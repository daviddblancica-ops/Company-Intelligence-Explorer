package cz.companyintel.web;

import cz.companyintel.service.AuditCsvExporter;
import cz.companyintel.service.AuditFilter;
import cz.companyintel.service.AuditService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;
    private final AuditCsvExporter auditCsvExporter;

    public AuditController(AuditService auditService, AuditCsvExporter auditCsvExporter) {
        this.auditService = auditService;
        this.auditCsvExporter = auditCsvExporter;
    }

    @GetMapping
    public List<ChangeEventResponse> recent(
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String severity,
            @RequestParam(defaultValue = "false") boolean archived,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long importRunId,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "100") int limit) {
        return auditService.find(filter(type, severity, archived, companyId, importRunId, query, from, to, limit)).stream()
                .map(ChangeEventResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String severity,
            @RequestParam(defaultValue = "false") boolean archived,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long importRunId,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AuditFilter filter = filter(type, severity, archived, companyId, importRunId, query, from, to, 5000);
        byte[] content = auditCsvExporter.export(auditService.find(filter)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=company-intelligence-audit.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(content);
    }

    @GetMapping("/types")
    public List<String> types() {
        return auditService.findTypes();
    }

    @PostMapping("/{id}/archive")
    public ChangeEventResponse setArchived(@PathVariable Long id, @RequestBody AuditArchiveRequest request) {
        return ChangeEventResponse.from(auditService.setArchived(id, request.isArchived()));
    }

    @PostMapping("/archive")
    public List<ChangeEventResponse> setArchived(@RequestBody AuditBulkArchiveRequest request) {
        return auditService.setArchived(request.getIds(), request.isArchived()).stream()
                .map(ChangeEventResponse::from)
                .collect(Collectors.toList());
    }

    private AuditFilter filter(
            String type,
            String severity,
            boolean archived,
            Long companyId,
            Long importRunId,
            String query,
            LocalDate from,
            LocalDate to,
            int limit) {
        return new AuditFilter(type, severity, archived, companyId, importRunId, query, from, to, limit);
    }
}
