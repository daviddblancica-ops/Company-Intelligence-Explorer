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
            updateDefaultLabels();
            return;
        }
        taskItemRepository.save(task("1. Stabilizovat jádro: health endpoint, chybové odpovědi, stav databáze", "Projekt", "HIGH", true));
        taskItemRepository.save(task("2. Ověřit import reálné firmy z ARES podle IČO", "Import", "HIGH", true));
        taskItemRepository.save(task("3. Dodělat registr lidí a detail osoby s vazbami na firmy", "Lidé", "HIGH", true));
        taskItemRepository.save(task("4. Rozšířit rychlé vyhledávání podle firmy, IČO, osoby a role", "Vyhledávání", "HIGH", true));
        taskItemRepository.save(task("5. Posílit audit: filtry, typy událostí, archiv a tiskový výpis", "Audit", "MEDIUM", true));
        taskItemRepository.save(task("6. Přidat historii importních běhů včetně chybných řádků", "Import", "MEDIUM", true));
        taskItemRepository.save(task("7. Zpřehlednit dashboard: metriky firem, osob, vazeb a watchlistu", "UI", "MEDIUM", true));
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
                .orElseThrow(() -> new ResourceNotFoundException("Úkol nebyl nalezen: " + id));
    }

    private String cleanTitle(String value) {
        String title = value == null ? "" : value.trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Název úkolu je povinný");
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

    private void updateDefaultLabels() {
        for (TaskItem task : taskItemRepository.findAll()) {
            String title = accentDefaultTitle(task.getTitle());
            String segment = accentSegment(task.getSegment());
            if (!title.equals(task.getTitle()) || !segment.equals(task.getSegment())) {
                task.update(title, segment, task.getPriority());
                taskItemRepository.save(task);
            }
        }
    }

    private String accentDefaultTitle(String title) {
        if (title == null) {
            return "";
        }
        return title
                .replace("1. Stabilizovat jadro: health endpoint, chybove odpovedi, stav databaze",
                        "1. Stabilizovat jádro: health endpoint, chybové odpovědi, stav databáze")
                .replace("2. Overit import realne firmy z ARES podle ICO",
                        "2. Ověřit import reálné firmy z ARES podle IČO")
                .replace("3. Dodelat registr lidi a detail osoby s vazbami na firmy",
                        "3. Dodělat registr lidí a detail osoby s vazbami na firmy")
                .replace("4. Rozsirit rychle vyhledavani podle firmy, ICO, osoby a role",
                        "4. Rozšířit rychlé vyhledávání podle firmy, IČO, osoby a role")
                .replace("5. Posilit audit: filtry, typy udalosti, archiv a tiskovy vypis",
                        "5. Posílit audit: filtry, typy událostí, archiv a tiskový výpis")
                .replace("6. Pridat historii importnich behu vcetne chybovych radku",
                        "6. Přidat historii importních běhů včetně chybných řádků")
                .replace("7. Zprehlednit dashboard: metriky firem, osob, vazeb a watchlistu",
                        "7. Zpřehlednit dashboard: metriky firem, osob, vazeb a watchlistu");
    }

    private String accentSegment(String segment) {
        if ("Lide".equals(segment)) {
            return "Lidé";
        }
        if ("Vyhledavani".equals(segment)) {
            return "Vyhledávání";
        }
        return segment;
    }
}
