package cz.companyintel.service;

import static org.assertj.core.api.Assertions.assertThat;

import cz.companyintel.domain.Company;
import cz.companyintel.web.CompanyRequest;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CompanyServiceTest {

    @Autowired
    private CompanyService companyService;

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
        request.setName("Central Trade Group a.s.");
        request.setRegistrationNumber("87654321");
        request.setCountry("CZ");
        request.setLegalForm("a.s.");

        companyService.saveCompany(request);

        assertThat(companyService.searchCompanies("central trade")).hasSize(1);
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
}
