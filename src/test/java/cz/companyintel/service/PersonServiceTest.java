package cz.companyintel.service;

import static org.assertj.core.api.Assertions.assertThat;

import cz.companyintel.domain.Company;
import cz.companyintel.domain.Person;
import cz.companyintel.repository.ChangeEventRepository;
import cz.companyintel.repository.PersonRepository;
import cz.companyintel.web.CompanyRequest;
import cz.companyintel.web.PersonUpdateRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PersonServiceTest {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PersonService personService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ChangeEventRepository changeEventRepository;

    @Test
    void updatesPersonalDetailsAndWritesAudit() {
        Company company = createCompany("Person Detail Company s.r.o.", "82233441");
        Company assigned = companyService.assignPerson(company.getId(), "Puvodni Jmeno", "jednatel");
        Long personId = assigned.getPeople().iterator().next().getPerson().getId();
        PersonUpdateRequest request = new PersonUpdateRequest();
        request.setFullName("Nove Jmeno");
        request.setDateOfBirth(LocalDate.of(1985, 4, 12));
        request.setResidenceAddress("Praha 1");
        request.setNote("Overeny kontakt");

        Person updated = personService.updatePerson(personId, request);

        assertThat(updated.getFullName()).isEqualTo("Nove Jmeno");
        assertThat(updated.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 4, 12));
        assertThat(updated.getResidenceAddress()).isEqualTo("Praha 1");
        assertThat(updated.getNote()).isEqualTo("Overeny kontakt");
        assertThat(companyService.getCompany(company.getId()).getChanges())
                .extracting("type")
                .contains("PERSON_UPDATED");
    }

    @Test
    void deletesPersonAndAllCompanyRelationships() {
        Company company = createCompany("Person Delete Company s.r.o.", "82233442");
        Company assigned = companyService.assignPerson(company.getId(), "Osoba Ke Smazani", "kontrolor");
        Long personId = assigned.getPeople().iterator().next().getPerson().getId();

        personService.deletePerson(personId);

        assertThat(personRepository.findById(personId)).isEmpty();
        assertThat(companyService.getCompany(company.getId()).getPeople()).isEmpty();
        assertThat(changeEventRepository.findAll())
                .extracting("type")
                .contains("PERSON_DELETED");
    }

    private Company createCompany(String name, String registrationNumber) {
        CompanyRequest request = new CompanyRequest();
        request.setName(name);
        request.setRegistrationNumber(registrationNumber);
        request.setCountry("CZ");
        request.setLegalForm("s.r.o.");
        return companyService.saveCompany(request);
    }
}
