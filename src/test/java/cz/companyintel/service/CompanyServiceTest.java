package cz.companyintel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cz.companyintel.domain.Company;
import cz.companyintel.repository.PersonRepository;
import cz.companyintel.web.CompanyRequest;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CompanyServiceTest {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PersonRepository personRepository;

    @Test
    void savesCompanyWithNormalizedNamePeopleAndChangeHistory() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Nova Data Systems s.r.o.");
        request.setRegistrationNumber("12345678");
        request.setCountry("CZ");
        request.setLegalForm("s.r.o.");

        CompanyRequest.PersonRole role = new CompanyRequest.PersonRole();
        role.setFullName("Jan Novak");
        role.setRole("jednatel");
        request.setPeople(Collections.singletonList(role));

        Company saved = companyService.saveCompany(request);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNormalizedName()).isEqualTo("nova data systems s r o");
        assertThat(saved.getPeople()).hasSize(1);
        assertThat(saved.getChanges()).hasSize(1);
    }

    @Test
    void searchesByNormalizedName() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Česká Trade Group a.s.");
        request.setRegistrationNumber("87654321");
        request.setCountry("CZ");
        request.setLegalForm("a.s.");

        companyService.saveCompany(request);

        assertThat(companyService.searchCompanies("ceska trade")).hasSize(1);
    }

    @Test
    void searchesByRegistrationNumberPersonAndRole() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Relationship Search s.r.o.");
        request.setRegistrationNumber("55544433");
        request.setCountry("CZ");
        request.setLegalForm("s.r.o.");

        CompanyRequest.PersonRole role = new CompanyRequest.PersonRole();
        role.setFullName("Michaela Strategicka");
        role.setRole("risk manager");
        request.setPeople(Collections.singletonList(role));

        companyService.saveCompany(request);

        assertThat(companyService.searchCompanies("55544433")).extracting("name").contains("Relationship Search s.r.o.");
        assertThat(companyService.searchCompanies("michaela")).extracting("name").contains("Relationship Search s.r.o.");
        assertThat(companyService.searchCompanies("manager")).extracting("name").contains("Relationship Search s.r.o.");
    }

    @Test
    void togglesWatchlistAndWritesHistory() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Watch Test s.r.o.");
        request.setRegistrationNumber("99911122");
        request.setCountry("CZ");
        request.setLegalForm("s.r.o.");

        Company saved = companyService.saveCompany(request);
        Company watched = companyService.setWatchlisted(saved.getId(), true);

        assertThat(watched.isWatchlisted()).isTrue();
        assertThat(watched.getChanges()).extracting("type").contains("WATCHLISTED");
    }

    @Test
    void assignsPersonToExistingCompanyAndWritesHistory() {
        CompanyRequest request = new CompanyRequest();
        request.setName("People Test s.r.o.");
        request.setRegistrationNumber("99911123");
        request.setCountry("CZ");
        request.setLegalForm("s.r.o.");

        Company saved = companyService.saveCompany(request);
        Company updated = companyService.assignPerson(saved.getId(), "Eva Prochazkova", "analyticka");

        assertThat(updated.getPeople()).hasSize(1);
        assertThat(updated.getPeople()).extracting("role").contains("analyticka");
        assertThat(updated.getChanges()).extracting("type").contains("PERSON_ASSIGNED");
    }

    @Test
    void updatesAssignedPersonRoleAndWritesHistory() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Role Update Test s.r.o.");
        request.setRegistrationNumber("99911125");
        request.setCountry("CZ");
        request.setLegalForm("s.r.o.");

        Company saved = companyService.saveCompany(request);
        Company assigned = companyService.assignPerson(saved.getId(), "Jana Role", "analyticka");
        Long personId = assigned.getPeople().iterator().next().getPerson().getId();

        Company updated = companyService.updatePersonRole(saved.getId(), personId, "jednatelka");

        assertThat(updated.getPeople()).hasSize(1);
        assertThat(updated.getPeople()).extracting("role").containsExactly("jednatelka");
        assertThat(updated.getChanges()).extracting("type").contains("PERSON_ROLE_UPDATED");
    }

    @Test
    void removesAssignedPersonAndWritesHistory() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Role Delete Test s.r.o.");
        request.setRegistrationNumber("99911126");
        request.setCountry("CZ");
        request.setLegalForm("s.r.o.");

        Company saved = companyService.saveCompany(request);
        Company assigned = companyService.assignPerson(saved.getId(), "Petr Remove", "kontrolor");
        Long personId = assigned.getPeople().iterator().next().getPerson().getId();

        Company updated = companyService.removePerson(saved.getId(), personId);

        assertThat(updated.getPeople()).isEmpty();
        assertThat(updated.getChanges()).extracting("type").contains("PERSON_REMOVED");
    }

    @Test
    void rejectsBlankPersonAssignmentBeforeCreatingPerson() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Blank People Test s.r.o.");
        request.setRegistrationNumber("99911124");
        request.setCountry("CZ");
        request.setLegalForm("s.r.o.");

        Company saved = companyService.saveCompany(request);
        long peopleBefore = personRepository.count();

        assertThatThrownBy(() -> companyService.assignPerson(saved.getId(), "   ", "kontrolor"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Person full name is required");
        assertThat(personRepository.count()).isEqualTo(peopleBefore);
    }
}
