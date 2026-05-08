package cz.companyintel.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(indexes = {
        @Index(name = "idx_import_run_started", columnList = "startedAt"),
        @Index(name = "idx_import_run_status", columnList = "status")
})
public class ImportRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceType;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private int totalRows;

    @Column(nullable = false)
    private int importedRows;

    @Column(nullable = false)
    private int failedRows;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "importRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ImportRowError> errors = new ArrayList<ImportRowError>();

    protected ImportRun() {
    }

    public ImportRun(String sourceType) {
        this.sourceType = sourceType;
        this.status = "RUNNING";
        this.startedAt = LocalDateTime.now();
    }

    public void addError(int rowNumber, String rawValue, String message) {
        errors.add(new ImportRowError(this, rowNumber, rawValue, message));
    }

    public void finish(int totalRows, int importedRows, int failedRows) {
        this.totalRows = totalRows;
        this.importedRows = importedRows;
        this.failedRows = failedRows;
        this.status = failedRows == 0 ? "SUCCESS" : importedRows == 0 ? "FAILED" : "PARTIAL";
        this.finishedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getStatus() {
        return status;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getImportedRows() {
        return importedRows;
    }

    public int getFailedRows() {
        return failedRows;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public List<ImportRowError> getErrors() {
        return errors;
    }
}
