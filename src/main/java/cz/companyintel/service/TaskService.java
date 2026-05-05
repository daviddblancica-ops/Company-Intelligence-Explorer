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
        taskItemRepository.save(new TaskItem("Zkontrolovat import CSV a JSON dat", "Import", "HIGH"));
        taskItemRepository.save(new TaskItem("Doplnit rychle vyhledavani pro vetsi objem dat", "Vyhledavani", "HIGH"));
        taskItemRepository.save(new TaskItem("Proverit prirazovani lidi k firmam", "Lide", "HIGH"));
        taskItemRepository.save(new TaskItem("Projit audit log, archivaci a tisk", "Audit", "MEDIUM"));
        taskItemRepository.save(new TaskItem("Doladit rozdeleni obrazovek podle menu", "UI", "MEDIUM"));
    }

    public List<TaskItem> findActive() {
        return taskItemRepository.findByArchivedOrderByDoneAscPriorityAscUpdatedAtDesc(false);
    }

    public List<TaskItem> findArchived() {
        return taskItemRepository.findByArchivedOrderByDoneAscPriorityAscUpdatedAtDesc(true);
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
