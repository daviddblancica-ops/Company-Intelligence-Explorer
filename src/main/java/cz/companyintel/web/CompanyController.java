package cz.companyintel.web;

import cz.companyintel.domain.Company;
import cz.companyintel.security.AuthorizationRules;
import cz.companyintel.service.CompanyService;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/companies")
@PreAuthorize(AuthorizationRules.READ)
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(AuthorizationRules.EDIT)
    public CompanyResponse save(@Valid @RequestBody CompanyRequest request) {
        return CompanyResponse.from(companyService.saveCompany(request));
    }

    @GetMapping("/{id}")
    public CompanyResponse get(@PathVariable Long id) {
        return CompanyResponse.from(companyService.getCompany(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(AuthorizationRules.EDIT)
    public CompanyResponse update(@PathVariable Long id, @Valid @RequestBody CompanyUpdateRequest request) {
        return CompanyResponse.from(companyService.updateCompany(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(AuthorizationRules.ADMIN)
    public void delete(@PathVariable Long id) {
        companyService.deleteCompany(id);
    }

    @GetMapping("/search")
    public List<CompanyResponse> search(@RequestParam(defaultValue = "") String q) {
        List<Company> companies = companyService.searchCompanies(q);
        return companies.stream().map(CompanyResponse::from).collect(Collectors.toList());
    }

    @PatchMapping("/{id}/watchlist")
    @PreAuthorize(AuthorizationRules.EDIT)
    public CompanyResponse setWatchlisted(@PathVariable Long id, @RequestBody WatchlistRequest request) {
        return CompanyResponse.from(companyService.setWatchlisted(id, request.isWatchlisted()));
    }

    @PostMapping("/{id}/people")
    @PreAuthorize(AuthorizationRules.EDIT)
    public CompanyResponse assignPerson(@PathVariable Long id, @Valid @RequestBody PersonAssignmentRequest request) {
        return CompanyResponse.from(companyService.assignPerson(id, request.getFullName(), request.getRole()));
    }

    @PatchMapping("/{id}/people/{personId}")
    @PreAuthorize(AuthorizationRules.EDIT)
    public CompanyResponse updatePersonRole(
            @PathVariable Long id,
            @PathVariable Long personId,
            @Valid @RequestBody PersonRoleUpdateRequest request) {
        return CompanyResponse.from(companyService.updatePersonRole(id, personId, request.getRole()));
    }

    @DeleteMapping("/{id}/people/{personId}")
    @PreAuthorize(AuthorizationRules.EDIT)
    public CompanyResponse removePerson(@PathVariable Long id, @PathVariable Long personId) {
        return CompanyResponse.from(companyService.removePerson(id, personId));
    }
}
