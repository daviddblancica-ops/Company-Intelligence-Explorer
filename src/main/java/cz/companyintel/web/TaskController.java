package cz.companyintel.web;

import cz.companyintel.domain.TaskItem;
import cz.companyintel.security.AuthorizationRules;
import cz.companyintel.service.TaskService;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@PreAuthorize(AuthorizationRules.READ)
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam(defaultValue = "false") boolean archived) {
        List<TaskItem> tasks = archived ? taskService.findArchived() : taskService.findActive();
        return tasks.stream().map(TaskResponse::from).collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(AuthorizationRules.EDIT)
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(AuthorizationRules.EDIT)
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.update(id, request));
    }

    @PatchMapping("/{id}/done")
    @PreAuthorize(AuthorizationRules.EDIT)
    public TaskResponse setDone(@PathVariable Long id, @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.setDone(id, request.isDone()));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize(AuthorizationRules.EDIT)
    public TaskResponse setArchived(@PathVariable Long id, @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.setArchived(id, request.isArchived()));
    }
}
