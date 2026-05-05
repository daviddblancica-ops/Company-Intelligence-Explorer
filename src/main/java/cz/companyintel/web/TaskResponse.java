package cz.companyintel.web;

import cz.companyintel.domain.TaskItem;
import java.time.LocalDateTime;

public class TaskResponse {

    private Long id;
    private String title;
    private String segment;
    private String priority;
    private boolean done;
    private boolean archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskResponse from(TaskItem task) {
        TaskResponse response = new TaskResponse();
        response.id = task.getId();
        response.title = task.getTitle();
        response.segment = task.getSegment();
        response.priority = task.getPriority();
        response.done = task.isDone();
        response.archived = task.isArchived();
        response.createdAt = task.getCreatedAt();
        response.updatedAt = task.getUpdatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSegment() {
        return segment;
    }

    public String getPriority() {
        return priority;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isArchived() {
        return archived;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
