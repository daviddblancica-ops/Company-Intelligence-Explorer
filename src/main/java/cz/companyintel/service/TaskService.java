package cz.companyintel.service;

import cz.companyintel.domain.TaskItem;
import cz.companyintel.repository.TaskItemRepository;
import cz.companyintel.web.TaskRequest;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskItemRepository taskItemRepository;

    public TaskService(TaskItemRepository taskItemRepository) {
        this.taskItemRepository = taskItemRepository;
    }

    @PostConstruct
    public void seedDefaults() {
        if (taskItemRepository.count() > 0) {
            return;
        }
        taskItemRepository.save(task("1. Stabilizovat jadro: health endpoint, chybove odpovedi, stav databaze", "Projekt", "HIGH", true));
        taskItemRepository.save(task("2. Pridat startup demo data pro firmy, osoby, vazby a audit", "Import", "HIGH", true));
        taskItemRepository.save(task("3. Dodelat registr lidi a detail osoby s vazbami na firmy", "Lide", "HIGH", true));
        taskItemRepository.save(task("4. Rozsirit rychle vyhledavani podle firmy, ICO, osoby a role", "Vyhledavani", "HIGH", true));
        taskItemRepository.save(task("5. Posilit audit: filtry, typy udalosti, archiv a tiskovy vypis", "Audit", "MEDIUM", true));
        taskItemRepository.save(task("6. Pridat historii importnich behu vcetne chybovych radku", "Import", "MEDIUM", true));
        taskItemRepository.save(task("7. Zprehlednit dashboard: metriky firem, osob, vazeb a watchlistu", "UI", "MEDIUM", false));
    }

    public List<TaskItem> findActive() {
        return taskItemRepository.findByArchivedOrderByDoneAscIdAsc(false);
    }

    public List<TaskItem> findArchived() {
        return taskItemRepository.findByArchivedOrderByDoneAscIdAsc(true);
    }

    private TaskItem task(String title, String segment, String priority, boolean done) {
        TaskItem task = new TaskItem(title, segment, priority);
        task.setDone(done);
        return task;
    }

    @Transactional
    public TaskItem create(TaskRequest request) {
        TaskItem task = new TaskItem(
                cleanTitle(request.getTitle()),
                cleanSegment(request.getSegment()),
                cleanPriority(request.getPriority()));
        return taskItemRepository.save(task);
    }

    @Transactional
    public TaskItem update(Long id, TaskRequest request) {
        TaskItem task = getTask(id);
        task.update(
                cleanTitle(request.getTitle()),
                cleanSegment(request.getSegment()),
                cleanPriority(request.getPriority()));
        return taskItemRepository.save(task);
    }

    @Transactional
    public TaskItem setDone(Long id, boolean done) {
        TaskItem task = getTask(id);
        task.setDone(done);
        return taskItemRepository.save(task);
    }

    @Transactional
    public TaskItem setArchived(Long id, boolean archived) {
        TaskItem task = getTask(id);
        task.setArchived(archived);
        return taskItemRepository.save(task);
    }

    private TaskItem getTask(Long id) {
        return taskItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    private String cleanTitle(String value) {
        String title = value == null ? "" : value.trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Task title is required");
        }
        return title.length() > 240 ? title.substring(0, 240) : title;
    }

    private String cleanSegment(String value) {
        String segment = value == null || value.trim().isEmpty() ? "Projekt" : value.trim();
        return segment.length() > 80 ? segment.substring(0, 80) : segment;
    }

    private String cleanPriority(String value) {
        String priority = value == null ? "MEDIUM" : value.trim().toUpperCase();
        if (!"HIGH".equals(priority) && !"MEDIUM".equals(priority) && !"LOW".equals(priority)) {
            return "MEDIUM";
        }
        return priority;
    }
}
