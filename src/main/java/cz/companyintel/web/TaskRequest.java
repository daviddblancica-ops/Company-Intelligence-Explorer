package cz.companyintel.web;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class TaskRequest {

    @NotBlank(message = "Název úkolu je povinný")
    @Size(max = 240, message = "Název úkolu může obsahovat nejvýše 240 znaků")
    private String title;

    @Size(max = 80, message = "Segment úkolu může obsahovat nejvýše 80 znaků")
    private String segment;

    @Pattern(regexp = "(?i)HIGH|MEDIUM|LOW", message = "Priorita musí být HIGH, MEDIUM nebo LOW")
    private String priority;
    private boolean done;
    private boolean archived;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
