package cz.companyintel.service;

import cz.companyintel.domain.CompanyPersonRole;
import cz.companyintel.domain.Person;
import cz.companyintel.repository.CompanyPersonRoleRepository;
import cz.companyintel.repository.PersonRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final CompanyPersonRoleRepository companyPersonRoleRepository;
    private final NormalizationService normalizationService;

    public PersonService(
            PersonRepository personRepository,
            CompanyPersonRoleRepository companyPersonRoleRepository,
            NormalizationService normalizationService) {
        this.personRepository = personRepository;
        this.companyPersonRoleRepository = companyPersonRoleRepository;
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
                .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + id));
    }

    public List<CompanyPersonRole> findRelationships(Long personId) {
        return companyPersonRoleRepository.findByPersonIdOrderByCompanyNameAsc(personId);
    }
}
