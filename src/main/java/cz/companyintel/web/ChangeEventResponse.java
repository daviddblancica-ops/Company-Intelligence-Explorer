package cz.companyintel.web;

import cz.companyintel.domain.ChangeEvent;
import java.time.LocalDateTime;

public class ChangeEventResponse {

    private Long id;
    private Long companyId;
    private Long importRunId;
    private String companyName;
    private String registrationNumber;
    private String type;
    private String severity;
    private String description;
    private LocalDateTime createdAt;
    private boolean archived;

    public static ChangeEventResponse from(ChangeEvent event) {
        ChangeEventResponse response = new ChangeEventResponse();
        response.id = event.getId();
        if (event.getCompany() != null) {
            response.companyId = event.getCompany().getId();
            response.companyName = event.getCompany().getName();
            response.registrationNumber = event.getCompany().getRegistrationNumber();
        }
        if (event.getImportRun() != null) {
            response.importRunId = event.getImportRun().getId();
        }
        response.type = event.getType();
        response.severity = event.getSeverity();
        response.description = event.getDescription();
        response.createdAt = event.getCreatedAt();
        response.archived = event.isArchived();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getImportRunId() {
        return importRunId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getType() {
        return type;
    }

    public String getSeverity() {
        return severity;
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
}
