package cz.companyintel.web;

import cz.companyintel.domain.ChangeEvent;
import java.time.LocalDateTime;

public class ChangeEventResponse {

    private Long id;
    private Long companyId;
    private String companyName;
    private String registrationNumber;
    private String type;
    private String severity;
    private String description;
    private LocalDateTime createdAt;

    public static ChangeEventResponse from(ChangeEvent event) {
        ChangeEventResponse response = new ChangeEventResponse();
        response.id = event.getId();
        response.companyId = event.getCompany().getId();
        response.companyName = event.getCompany().getName();
        response.registrationNumber = event.getCompany().getRegistrationNumber();
        response.type = event.getType();
        response.severity = severity(event.getType());
        response.description = event.getDescription();
        response.createdAt = event.getCreatedAt();
        return response;
    }

    private static String severity(String type) {
        if (type == null) {
            return "INFO";
        }
        if (type.contains("FAILED") || type.contains("ERROR")) {
            return "CRITICAL";
        }
        if (type.contains("WATCHLIST") || type.contains("PERSON")) {
            return "WARNING";
        }
        return "INFO";
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
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
}
