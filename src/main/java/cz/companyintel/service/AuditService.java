package cz.companyintel.service;

import cz.companyintel.domain.ChangeEvent;
import cz.companyintel.domain.ImportRun;
import cz.companyintel.repository.ChangeEventRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;

@Service
public class AuditService {

    private final ChangeEventRepository changeEventRepository;

    public AuditService(ChangeEventRepository changeEventRepository) {
        this.changeEventRepository = changeEventRepository;
    }

    public List<ChangeEvent> findRecent(String type, boolean archived, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        PageRequest page = PageRequest.of(0, safeLimit);
        if (type == null || type.trim().isEmpty()) {
            return changeEventRepository.findByArchivedOrderByCreatedAtDesc(archived, page);
        }
        return changeEventRepository.findByTypeAndArchivedOrderByCreatedAtDesc(type.trim().toUpperCase(), archived, page);
    }

    public List<String> findTypes() {
        return changeEventRepository.findAllByOrderByTypeAsc().stream()
                .map(ChangeEvent::getType)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public ChangeEvent recordImportRun(ImportRun run) {
        String type = "IMPORT_" + run.getStatus();
        String description = "Import run #" + run.getId()
                + " (" + run.getSourceType() + ") finished with status " + run.getStatus()
                + ": imported " + run.getImportedRows()
                + ", failed " + run.getFailedRows()
                + ", total " + run.getTotalRows() + ".";
        return changeEventRepository.save(new ChangeEvent(run, type, description));
    }

    @Transactional
    public ChangeEvent setArchived(Long id, boolean archived) {
        ChangeEvent event = changeEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit event not found: " + id));
        event.setArchived(archived);
        return changeEventRepository.save(event);
    }
}
