package cz.companyintel.service;

import cz.companyintel.domain.CompanyPersonRole;
import cz.companyintel.domain.ChangeEvent;
import cz.companyintel.domain.Company;
import cz.companyintel.domain.Person;
import cz.companyintel.repository.ChangeEventRepository;
import cz.companyintel.repository.CompanyRepository;
import cz.companyintel.repository.CompanyPersonRoleRepository;
import cz.companyintel.repository.PersonRepository;
import cz.companyintel.web.PersonUpdateRequest;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final CompanyPersonRoleRepository companyPersonRoleRepository;
    private final CompanyRepository companyRepository;
    private final ChangeEventRepository changeEventRepository;
    private final NormalizationService normalizationService;

    public PersonService(
            PersonRepository personRepository,
            CompanyPersonRoleRepository companyPersonRoleRepository,
            CompanyRepository companyRepository,
            ChangeEventRepository changeEventRepository,
            NormalizationService normalizationService) {
        this.personRepository = personRepository;
        this.companyPersonRoleRepository = companyPersonRoleRepository;
        this.companyRepository = companyRepository;
        this.changeEventRepository = changeEventRepository;
        this.normalizationService = normalizationService;
    }

    public List<Person> searchPeople(String query) {
        Sort sort = Sort.by("fullName").ascending();
        String normalizedQuery = normalizationService.normalizeName(query);
        if (normalizedQuery.isEmpty()) {
            return personRepository.findAll(sort);
        }
        return personRepository.findByNormalizedNameContaining(normalizedQuery, sort);
    }

    public Person getPerson(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Osoba nebyla nalezena: " + id));
    }

    public List<CompanyPersonRole> findRelationships(Long personId) {
        return companyPersonRoleRepository.findByPersonIdOrderByCompanyNameAsc(personId);
    }

    @Transactional
    public Person updatePerson(Long id, PersonUpdateRequest request) {
        Person person = getPerson(id);
        String fullName = required(request.getFullName(), "Jméno osoby je povinné");
        if (request.getDateOfBirth() != null && request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Datum narození nemůže být v budoucnosti");
        }
        String normalizedName = normalizationService.normalizeName(fullName);
        personRepository.findByNormalizedName(normalizedName)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Osoba s tímto jménem již existuje");
                });

        String previousName = person.getFullName();
        person.updateProfile(
                fullName,
                normalizedName,
                request.getDateOfBirth(),
                clean(request.getResidenceAddress()),
                clean(request.getNote()));
        Person saved = personRepository.save(person);
        Set<Company> companies = companiesFor(id);
        for (Company company : companies) {
            company.addChange("PERSON_UPDATED", previousName + " změněn na " + fullName);
        }
        companyRepository.saveAll(companies);
        changeEventRepository.save(new ChangeEvent(
                null,
                null,
                "PERSON_UPDATED",
                "Profil osoby byl upraven: " + previousName + " -> " + fullName));
        return saved;
    }

    @Transactional
    public void deletePerson(Long id) {
        Person person = getPerson(id);
        String fullName = person.getFullName();
        List<CompanyPersonRole> relationships = findRelationships(id);
        Set<Company> companies = new LinkedHashSet<Company>();
        for (CompanyPersonRole relationship : relationships) {
            Company company = relationship.getCompany();
            company.removeRole(id);
            company.addChange("PERSON_DELETED", fullName + " smazán z registru osob");
            companies.add(company);
        }
        companyRepository.saveAll(companies);
        companyRepository.flush();
        personRepository.delete(person);
        personRepository.flush();
        changeEventRepository.save(new ChangeEvent(
                null,
                null,
                "PERSON_DELETED",
                "Osoba byla smazána z registru: " + fullName + " (odstraněné vazby: "
                        + relationships.size() + ")"));
    }

    private Set<Company> companiesFor(Long personId) {
        Set<Company> companies = new LinkedHashSet<Company>();
        for (CompanyPersonRole relationship : findRelationships(personId)) {
            companies.add(relationship.getCompany());
        }
        return companies;
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
