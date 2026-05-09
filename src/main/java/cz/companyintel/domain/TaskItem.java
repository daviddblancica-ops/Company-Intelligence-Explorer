package cz.companyintel.domain;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(indexes = {
        @Index(name = "idx_task_done", columnList = "done"),
        @Index(name = "idx_task_archived", columnList = "archived"),
        @Index(name = "idx_task_segment", columnList = "segment")
})
public class TaskItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false)
    private String segment;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private boolean done;

    @Column(nullable = false)
    private boolean archived;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected TaskItem() {
    }

    public TaskItem(String title, String segment, String priority) {
        LocalDateTime now = LocalDateTime.now();
        this.title = title;
        this.segment = segment;
        this.priority = priority;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String segment, String priority) {
        this.title = title;
        this.segment = segment;
        this.priority = priority;
        this.updatedAt = LocalDateTime.now();
    }

    public void setDone(boolean done) {
        this.done = done;
        this.updatedAt = LocalDateTime.now();
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
        this.updatedAt = LocalDateTime.now();
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
