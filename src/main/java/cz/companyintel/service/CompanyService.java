package cz.companyintel.service;

import cz.companyintel.domain.Company;
import cz.companyintel.domain.ChangeEvent;
import cz.companyintel.domain.Person;
import cz.companyintel.repository.ChangeEventRepository;
import cz.companyintel.repository.CompanyRepository;
import cz.companyintel.repository.PersonRepository;
import cz.companyintel.web.CompanyRequest;
import cz.companyintel.web.CompanyUpdateRequest;
import java.util.ArrayList;
import java.util.List;
import javax.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final PersonRepository personRepository;
    private final ChangeEventRepository changeEventRepository;
    private final NormalizationService normalizationService;

    public CompanyService(
            CompanyRepository companyRepository,
            PersonRepository personRepository,
            ChangeEventRepository changeEventRepository,
            NormalizationService normalizationService) {
        this.companyRepository = companyRepository;
        this.personRepository = personRepository;
        this.changeEventRepository = changeEventRepository;
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

        company.addChange(existing ? "UPDATED" : "CREATED",
                existing ? "Profil firmy byl aktualizován" : "Firma byla importována");
        return companyRepository.save(company);
    }

    public Company getCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Firma nebyla nalezena: " + id));
    }

    @Transactional
    public Company updateCompany(Long id, CompanyUpdateRequest request) {
        Company company = getCompany(id);
        String name = required(request.getName(), "Název firmy je povinný");
        String registrationNumber = required(request.getRegistrationNumber(), "IČO je povinné");
        String normalizedName = normalizationService.normalizeName(name);
        companyRepository.findByRegistrationNumber(registrationNumber)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("IČO již patří jiné firmě");
                });

        company.updateProfile(
                name,
                normalizedName,
                registrationNumber,
                clean(request.getCountry()),
                clean(request.getLegalForm()),
                clean(request.getAddress()),
                clean(request.getDataSource()));
        company.addChange("UPDATED", "Profil firmy byl ručně upraven");
        return companyRepository.save(company);
    }

    @Transactional
    public void deleteCompany(Long id) {
        Company company = getCompany(id);
        String name = company.getName();
        String registrationNumber = company.getRegistrationNumber();
        List<ChangeEvent> history = new ArrayList<ChangeEvent>(company.getChanges());
        for (ChangeEvent event : history) {
            event.detachCompany();
        }
        changeEventRepository.saveAll(history);
        company.getChanges().clear();
        companyRepository.delete(company);
        companyRepository.flush();
        changeEventRepository.save(new ChangeEvent(
                name,
                registrationNumber,
                "COMPANY_DELETED",
                "Firma byla smazána z registru: " + name + " (" + registrationNumber + ")"));
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
                watchlisted ? "Firma byla přidána na watchlist" : "Firma byla odebrána z watchlistu");
        return companyRepository.save(company);
    }

    @Transactional
    public Company assignPerson(Long companyId, String fullName, String role) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Jméno osoby je povinné");
        }
        Company company = getCompany(companyId);
        Person person = findOrCreatePerson(fullName.trim());
        String normalizedRole = role == null || role.trim().isEmpty() ? "role neuvedena" : role.trim();
        company.replaceRole(person, normalizedRole);
        company.addChange("PERSON_ASSIGNED", person.getFullName() + " přiřazen jako " + normalizedRole);
        return companyRepository.save(company);
    }

    @Transactional
    public Company updatePersonRole(Long companyId, Long personId, String role) {
        Company company = getCompany(companyId);
        String normalizedRole = role == null || role.trim().isEmpty() ? "role neuvedena" : role.trim();
        if (!company.updateRole(personId, normalizedRole)) {
            throw new ResourceNotFoundException("Přiřazení osoby nebylo nalezeno: " + personId);
        }
        company.addChange("PERSON_ROLE_UPDATED", "Role osoby byla změněna na " + normalizedRole);
        return companyRepository.save(company);
    }

    @Transactional
    public Company removePerson(Long companyId, Long personId) {
        Company company = getCompany(companyId);
        if (!company.removeRole(personId)) {
            throw new ResourceNotFoundException("Přiřazení osoby nebylo nalezeno: " + personId);
        }
        company.addChange("PERSON_REMOVED", "Osoba byla odebrána od firmy");
        return companyRepository.save(company);
    }

    private Person findOrCreatePerson(String fullName) {
        String normalizedName = normalizationService.normalizeName(fullName);
        return personRepository.findByNormalizedName(normalizedName)
                .orElseGet(() -> personRepository.save(new Person(fullName, normalizedName)));
    }

    private String required(String value, String message) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }

    private String clean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
