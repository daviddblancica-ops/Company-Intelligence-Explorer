package cz.companyintel.web;

import cz.companyintel.security.AuthorizationRules;
import cz.companyintel.service.HealthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@PreAuthorize(AuthorizationRules.READ)
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public HealthResponse current() {
        return healthService.current();
    }
}
