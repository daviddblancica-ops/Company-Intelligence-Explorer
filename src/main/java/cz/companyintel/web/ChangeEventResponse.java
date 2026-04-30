package cz.companyintel.web;

import cz.companyintel.domain.ChangeEvent;
import java.time.LocalDateTime;

public class ChangeEventResponse {

    private Long id;
    private String type;
    private String description;
    private LocalDateTime createdAt;

    public static ChangeEventResponse from(ChangeEvent event) {
        ChangeEventResponse response = new ChangeEventResponse();
        response.id = event.getId();
        response.type = event.getType();
        response.description = event.getDescription();
        response.createdAt = event.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
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
}
