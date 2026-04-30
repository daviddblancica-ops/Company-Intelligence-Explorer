package cz.companyintel.web;

import cz.companyintel.domain.Company;
import cz.companyintel.service.CompanyService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/search")
    public List<CompanyResponse> search(@RequestParam String q) {
        List<Company> companies = companyService.searchCompanies(q);
        return companies.stream().map(CompanyResponse::from).collect(Collectors.toList());
    }
}
