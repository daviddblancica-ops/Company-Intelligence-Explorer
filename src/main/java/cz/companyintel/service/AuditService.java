package cz.companyintel.service;

import cz.companyintel.domain.ChangeEvent;
import cz.companyintel.domain.ImportRun;
import cz.companyintel.repository.ChangeEventRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    public List<ChangeEvent> find(AuditFilter filter) {
        PageRequest page = PageRequest.of(0, filter.getLimit());
        return changeEventRepository.search(
                filter.getType(),
                filter.getSeverity(),
                filter.isArchived(),
                filter.getCompanyId(),
                filter.getImportRunId(),
                filter.getQuery(),
                filter.getFromDateTime(),
                filter.getToExclusiveDateTime(),
                page);
    }

    public List<String> findTypes() {
        return changeEventRepository.findDistinctTypes();
    }

    @Transactional
    public ChangeEvent recordImportRun(ImportRun run) {
        String type = "IMPORT_" + run.getStatus();
        String description = "Importní běh #" + run.getId()
                + " (" + run.getSourceType() + ") dokončen se stavem " + run.getStatus()
                + ": importováno " + run.getImportedRows()
                + ", chybně " + run.getFailedRows()
                + ", celkem " + run.getTotalRows() + ".";
        return changeEventRepository.save(new ChangeEvent(run, type, description));
    }

    @Transactional
    public ChangeEvent setArchived(Long id, boolean archived) {
        ChangeEvent event = changeEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auditní událost nebyla nalezena: " + id));
        event.setArchived(archived);
        return changeEventRepository.save(event);
    }

    @Transactional
    public List<ChangeEvent> setArchived(List<Long> ids, boolean archived) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Je vyžadováno alespoň jedno ID auditní události");
        }
        Set<Long> uniqueIds = ids.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniqueIds.isEmpty() || uniqueIds.size() > 500) {
            throw new IllegalArgumentException("Požadavek na archivaci musí obsahovat 1 až 500 ID událostí");
        }
        List<ChangeEvent> events = changeEventRepository.findAllById(uniqueIds);
        if (events.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException("Jedna nebo více auditních událostí nebylo nalezeno");
        }
        for (ChangeEvent event : events) {
            event.setArchived(archived);
        }
        return changeEventRepository.saveAll(events);
    }
}
