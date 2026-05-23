package cz.companyintel.domain;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(indexes = {
        @Index(name = "idx_change_company_created", columnList = "company_id, createdAt"),
        @Index(name = "idx_change_import_run_created", columnList = "import_run_id, createdAt"),
        @Index(name = "idx_change_type_created", columnList = "type, createdAt"),
        @Index(name = "idx_change_archived_created", columnList = "archived, createdAt")
})
public class ChangeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne
    @JoinColumn(name = "import_run_id")
    private ImportRun importRun;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 1200)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean archived;

    protected ChangeEvent() {
    }

    public ChangeEvent(Company company, String type, String description) {
        this.company = company;
        this.type = type;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public ChangeEvent(ImportRun importRun, String type, String description) {
        this.importRun = importRun;
        this.type = type;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public ImportRun getImportRun() {
        return importRun;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
