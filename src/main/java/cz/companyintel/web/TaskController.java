package cz.companyintel.web;

import cz.companyintel.domain.TaskItem;
import cz.companyintel.service.TaskService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
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
    public TaskResponse create(@RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.create(request));
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.update(id, request));
    }

    @PatchMapping("/{id}/done")
    public TaskResponse setDone(@PathVariable Long id, @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.setDone(id, request.isDone()));
    }

    @PatchMapping("/{id}/archive")
    public TaskResponse setArchived(@PathVariable Long id, @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.setArchived(id, request.isArchived()));
    }
}
