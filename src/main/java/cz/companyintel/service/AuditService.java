package cz.companyintel.service;

import cz.companyintel.domain.ChangeEvent;
import cz.companyintel.repository.ChangeEventRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final ChangeEventRepository changeEventRepository;

    public AuditService(ChangeEventRepository changeEventRepository) {
        this.changeEventRepository = changeEventRepository;
    }

    public List<ChangeEvent> findRecent(String type, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        PageRequest page = PageRequest.of(0, safeLimit);
        if (type == null || type.trim().isEmpty()) {
            return changeEventRepository.findAllByOrderByCreatedAtDesc(page);
        }
        return changeEventRepository.findByTypeOrderByCreatedAtDesc(type.trim().toUpperCase(), page);
    }
}
