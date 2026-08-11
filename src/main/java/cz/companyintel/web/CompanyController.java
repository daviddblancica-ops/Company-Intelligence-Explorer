package cz.companyintel.web;

import cz.companyintel.domain.Company;
import cz.companyintel.service.CompanyService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
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
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse save(@RequestBody CompanyRequest request) {
        return CompanyResponse.from(companyService.saveCompany(request));
    }

    @GetMapping("/{id}")
    public CompanyResponse get(@PathVariable Long id) {
        return CompanyResponse.from(companyService.getCompany(id));
    }

    @PutMapping("/{id}")
    public CompanyResponse update(@PathVariable Long id, @RequestBody CompanyUpdateRequest request) {
        return CompanyResponse.from(companyService.updateCompany(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        companyService.deleteCompany(id);
    }

    @GetMapping("/search")
    public List<CompanyResponse> search(@RequestParam(defaultValue = "") String q) {
        List<Company> companies = companyService.searchCompanies(q);
        return companies.stream().map(CompanyResponse::from).collect(Collectors.toList());
    }

    @PatchMapping("/{id}/watchlist")
    public CompanyResponse setWatchlisted(@PathVariable Long id, @RequestBody WatchlistRequest request) {
        return CompanyResponse.from(companyService.setWatchlisted(id, request.isWatchlisted()));
    }

    @PostMapping("/{id}/people")
    public CompanyResponse assignPerson(@PathVariable Long id, @RequestBody PersonAssignmentRequest request) {
        return CompanyResponse.from(companyService.assignPerson(id, request.getFullName(), request.getRole()));
    }

    @PatchMapping("/{id}/people/{personId}")
    public CompanyResponse updatePersonRole(
            @PathVariable Long id,
            @PathVariable Long personId,
            @RequestBody PersonAssignmentRequest request) {
        return CompanyResponse.from(companyService.updatePersonRole(id, personId, request.getRole()));
    }

    @DeleteMapping("/{id}/people/{personId}")
    public CompanyResponse removePerson(@PathVariable Long id, @PathVariable Long personId) {
        return CompanyResponse.from(companyService.removePerson(id, personId));
    }
}
