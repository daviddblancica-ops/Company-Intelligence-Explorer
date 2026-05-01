package cz.companyintel.service;

import cz.companyintel.domain.Company;
import cz.companyintel.domain.Person;
import cz.companyintel.repository.CompanyRepository;
import cz.companyintel.repository.PersonRepository;
import cz.companyintel.web.CompanyRequest;
import java.util.List;
import javax.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final PersonRepository personRepository;
    private final NormalizationService normalizationService;

    public CompanyService(
            CompanyRepository companyRepository,
            PersonRepository personRepository,
            NormalizationService normalizationService) {
        this.companyRepository = companyRepository;
        this.personRepository = personRepository;
        this.normalizationService = normalizationService;
    }

    @Transactional
    public Company saveCompany(CompanyRequest request) {
        String normalizedCompanyName = normalizationService.normalizeName(request.getName());
        Company company = companyRepository.findByRegistrationNumber(request.getRegistrationNumber())
                .orElseGet(() -> new Company(
                        request.getName(),
                        normalizedCompanyName,
                        request.getRegistrationNumber(),
                        request.getCountry(),
                        request.getLegalForm()));

        boolean existing = company.getId() != null;
        company.updateProfile(
                request.getName(),
                normalizedCompanyName,
                request.getCountry(),
                request.getLegalForm(),
                request.getAddress(),
                request.getDataSource());

        if (request.getPeople() != null) {
            company.clearRoles();
            for (CompanyRequest.PersonRole personRole : request.getPeople()) {
                Person person = findOrCreatePerson(personRole.getFullName());
                company.addRole(person, personRole.getRole());
            }
        }

        company.addChange(existing ? "UPDATED" : "CREATED", existing ? "Company profile updated" : "Company imported");
        return companyRepository.save(company);
    }

    public Company getCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));
    }

    public List<Company> searchCompanies(String query) {
        String normalizedQuery = normalizationService.normalizeName(query);
        return companyRepository.search(normalizedQuery, PageRequest.of(0, 100));
    }

    @Transactional
    public Company setWatchlisted(Long id, boolean watchlisted) {
        Company company = getCompany(id);
        company.setWatchlisted(watchlisted);
        company.addChange(
                watchlisted ? "WATCHLISTED" : "UNWATCHLISTED",
                watchlisted ? "Company added to watchlist" : "Company removed from watchlist");
        return companyRepository.save(company);
    }

    @Transactional
    public Company assignPerson(Long companyId, String fullName, String role) {
        Company company = getCompany(companyId);
        Person person = findOrCreatePerson(fullName);
        String normalizedRole = role == null || role.trim().isEmpty() ? "role neuvedena" : role.trim();
        company.replaceRole(person, normalizedRole);
        company.addChange("PERSON_ASSIGNED", person.getFullName() + " assigned as " + normalizedRole);
        return companyRepository.save(company);
    }

    private Person findOrCreatePerson(String fullName) {
        String normalizedName = normalizationService.normalizeName(fullName);
        return personRepository.findByNormalizedName(normalizedName)
                .orElseGet(() -> personRepository.save(new Person(fullName, normalizedName)));
    }
}
