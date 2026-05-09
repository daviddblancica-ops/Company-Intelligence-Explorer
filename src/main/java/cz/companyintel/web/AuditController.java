package cz.companyintel.web;

import cz.companyintel.service.AuditService;
import java.util.List;
import java.util.stream.Collectors;
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

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<ChangeEventResponse> recent(
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "false") boolean archived,
            @RequestParam(defaultValue = "100") int limit) {
        return auditService.findRecent(type, archived, limit).stream()
                .map(ChangeEventResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/types")
    public List<String> types() {
        return auditService.findTypes();
    }

    @PostMapping("/{id}/archive")
    public ChangeEventResponse setArchived(@PathVariable Long id, @RequestBody AuditArchiveRequest request) {
        return ChangeEventResponse.from(auditService.setArchived(id, request.isArchived()));
    }
}
