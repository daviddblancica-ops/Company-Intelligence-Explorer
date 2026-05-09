package cz.companyintel.service;

import cz.companyintel.repository.ChangeEventRepository;
import cz.companyintel.repository.CompanyPersonRoleRepository;
import cz.companyintel.repository.CompanyRepository;
import cz.companyintel.repository.ImportRunRepository;
import cz.companyintel.repository.PersonRepository;
import cz.companyintel.web.DashboardResponse;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final CompanyRepository companyRepository;
    private final PersonRepository personRepository;
    private final CompanyPersonRoleRepository companyPersonRoleRepository;
    private final ChangeEventRepository changeEventRepository;
    private final ImportRunRepository importRunRepository;

    public DashboardService(
            CompanyRepository companyRepository,
            PersonRepository personRepository,
            CompanyPersonRoleRepository companyPersonRoleRepository,
            ChangeEventRepository changeEventRepository,
            ImportRunRepository importRunRepository) {
        this.companyRepository = companyRepository;
        this.personRepository = personRepository;
        this.companyPersonRoleRepository = companyPersonRoleRepository;
        this.changeEventRepository = changeEventRepository;
        this.importRunRepository = importRunRepository;
    }

    public DashboardResponse current() {
        return new DashboardResponse(
                companyRepository.count(),
                personRepository.count(),
                companyPersonRoleRepository.count(),
                companyRepository.countByWatchlistedTrue(),
                changeEventRepository.count(),
                importRunRepository.count(),
                LocalDateTime.now());
    }
}
