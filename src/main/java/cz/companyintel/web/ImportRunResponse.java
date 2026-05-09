package cz.companyintel.web;

import cz.companyintel.domain.ImportRun;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ImportRunResponse {

    private Long id;
    private String sourceType;
    private String status;
    private int totalRows;
    private int importedRows;
    private int failedRows;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<ImportRowErrorResponse> errors;

    public static ImportRunResponse from(ImportRun run) {
        ImportRunResponse response = new ImportRunResponse();
        response.id = run.getId();
        response.sourceType = run.getSourceType();
        response.status = run.getStatus();
        response.totalRows = run.getTotalRows();
        response.importedRows = run.getImportedRows();
        response.failedRows = run.getFailedRows();
        response.startedAt = run.getStartedAt();
        response.finishedAt = run.getFinishedAt();
        response.errors = run.getErrors().stream()
                .map(ImportRowErrorResponse::from)
                .collect(Collectors.toList());
        return response;
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

    public List<ImportRowErrorResponse> getErrors() {
        return errors;
    }
}
