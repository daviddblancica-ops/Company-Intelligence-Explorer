package cz.companyintel.service;

import cz.companyintel.domain.Company;
import cz.companyintel.repository.CompanyRepository;
import cz.companyintel.web.CompanyRequest;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class DemoDataService {

    private final CompanyRepository companyRepository;
    private final CompanyService companyService;

    public DemoDataService(CompanyRepository companyRepository, CompanyService companyService) {
        this.companyRepository = companyRepository;
        this.companyService = companyService;
    }

    @PostConstruct
    @Transactional
    public void seedDefaults() {
        if (companyRepository.count() > 0) {
            return;
        }

        Company atlas = companyService.saveCompany(company(
                "Atlas Data Lab s.r.o.",
                "70010001",
                "s.r.o.",
                "Na Porici 12, Praha",
                person("Michaela Cerna", "jednatelka"),
                person("Karel Novak", "datovy analytik")));
        atlas.addChange("DEMO_DATA", "Startup demo data created for company intelligence workflow");
        companyRepository.save(atlas);
        companyService.setWatchlisted(atlas.getId(), true);

        Company bridge = companyService.saveCompany(company(
                "Bridge Advisory a.s.",
                "70010002",
                "a.s.",
                "Hlinky 48, Brno",
                person("Lucie Hruba", "clen predstavenstva")));
        bridge.addChange("DEMO_DATA", "Startup demo data created with person relationship");
        companyRepository.save(bridge);

        Company meridian = companyService.saveCompany(company(
                "Meridian Trade s.r.o.",
                "70010003",
                "s.r.o.",
                "Sokolovska 88, Plzen",
                person("Pavel Urban", "obchodni reditel")));
        meridian.addChange("DEMO_DATA", "Startup demo data created for search and audit examples");
        companyRepository.save(meridian);
    }

    private CompanyRequest company(
            String name,
            String registrationNumber,
            String legalForm,
            String address,
            CompanyRequest.PersonRole... people) {
        CompanyRequest request = new CompanyRequest();
        request.setName(name);
        request.setRegistrationNumber(registrationNumber);
        request.setCountry("CZ");
        request.setLegalForm(legalForm);
        request.setAddress(address);
        request.setDataSource("DEMO");
        for (CompanyRequest.PersonRole person : people) {
            request.getPeople().add(person);
        }
        return request;
    }

    private CompanyRequest.PersonRole person(String fullName, String role) {
        CompanyRequest.PersonRole person = new CompanyRequest.PersonRole();
        person.setFullName(fullName);
        person.setRole(role);
        return person;
    }
}
