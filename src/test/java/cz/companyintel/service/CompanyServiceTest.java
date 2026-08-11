package cz.companyintel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cz.companyintel.domain.Company;
import cz.companyintel.domain.ChangeEvent;
import cz.companyintel.repository.ChangeEventRepository;
import cz.companyintel.repository.CompanyRepository;
import cz.companyintel.repository.PersonRepository;
import cz.companyintel.web.CompanyRequest;
import cz.companyintel.web.CompanyUpdateRequest;
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

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ChangeEventRepository changeEventRepository;

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
                .hasMessage("Jméno osoby je povinné");
        assertThat(personRepository.count()).isEqualTo(peopleBefore);
    }

    @Test
    void updatesCompanyProfileWithoutReplacingPeople() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Editable Company s.r.o.");
        request.setRegistrationNumber("81122334");
        request.setCountry("CZ");
        request.setLegalForm("s.r.o.");
        Company saved = companyService.saveCompany(request);
        companyService.assignPerson(saved.getId(), "Editovana Osoba", "jednatel");

        CompanyUpdateRequest update = new CompanyUpdateRequest();
        update.setName("Editable Company a.s.");
        update.setRegistrationNumber("81122335");
        update.setCountry("CZ");
        update.setLegalForm("a.s.");
        update.setAddress("Testovaci 10, Praha");
        update.setDataSource("MANUAL");

        Company updated = companyService.updateCompany(saved.getId(), update);

        assertThat(updated.getName()).isEqualTo("Editable Company a.s.");
        assertThat(updated.getRegistrationNumber()).isEqualTo("81122335");
        assertThat(updated.getAddress()).isEqualTo("Testovaci 10, Praha");
        assertThat(updated.getPeople()).hasSize(1);
        assertThat(updated.getChanges()).extracting("type").contains("UPDATED");
    }

    @Test
    void reimportWithoutPeopleKeepsManualRelationships() {
        CompanyRequest original = new CompanyRequest();
        original.setName("ARES Relationship Company s.r.o.");
        original.setRegistrationNumber("81122337");
        original.setCountry("CZ");
        original.setLegalForm("s.r.o.");
        Company saved = companyService.saveCompany(original);
        companyService.assignPerson(saved.getId(), "Manualni Vazba", "jednatel");

        CompanyRequest reimport = new CompanyRequest();
        reimport.setName("ARES Relationship Company s.r.o.");
        reimport.setRegistrationNumber("81122337");
        reimport.setCountry("CZ");
        reimport.setLegalForm("s.r.o.");
        reimport.setDataSource("ARES");

        Company updated = companyService.saveCompany(reimport);

        assertThat(updated.getPeople()).hasSize(1);
        assertThat(updated.getPeople()).extracting("role").containsExactly("jednatel");
    }

    @Test
    void deletesCompanyButPreservesAuditHistory() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Deleted Audit Company s.r.o.");
        request.setRegistrationNumber("81122336");
        request.setCountry("CZ");
        request.setLegalForm("s.r.o.");
        Company saved = companyService.saveCompany(request);
        Long companyId = saved.getId();

        companyService.deleteCompany(companyId);

        assertThat(companyRepository.findById(companyId)).isEmpty();
        assertThat(changeEventRepository.findAll())
                .filteredOn(event -> "81122336".equals(event.getRegistrationNumber()))
                .extracting(ChangeEvent::getType)
                .contains("CREATED", "COMPANY_DELETED");
        assertThat(changeEventRepository.findAll())
                .filteredOn(event -> "81122336".equals(event.getRegistrationNumber()))
                .allMatch(event -> event.getCompany() == null);
    }
}
