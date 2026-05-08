package cz.companyintel.service;

import cz.companyintel.repository.ChangeEventRepository;
import cz.companyintel.repository.CompanyRepository;
import cz.companyintel.repository.PersonRepository;
import cz.companyintel.repository.TaskItemRepository;
import cz.companyintel.web.HealthResponse;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final CompanyRepository companyRepository;
    private final PersonRepository personRepository;
    private final ChangeEventRepository changeEventRepository;
    private final TaskItemRepository taskItemRepository;

    public HealthService(
            CompanyRepository companyRepository,
            PersonRepository personRepository,
            ChangeEventRepository changeEventRepository,
            TaskItemRepository taskItemRepository) {
        this.companyRepository = companyRepository;
        this.personRepository = personRepository;
        this.changeEventRepository = changeEventRepository;
        this.taskItemRepository = taskItemRepository;
    }

    public HealthResponse current() {
        return new HealthResponse(
                "UP",
                "UP",
                companyRepository.count(),
                personRepository.count(),
                changeEventRepository.count(),
                taskItemRepository.count(),
                LocalDateTime.now());
    }
}
