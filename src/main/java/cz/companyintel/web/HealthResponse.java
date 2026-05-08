package cz.companyintel.web;

import java.time.LocalDateTime;

public class HealthResponse {

    private final String status;
    private final String database;
    private final long companies;
    private final long people;
    private final long auditEvents;
    private final long tasks;
    private final LocalDateTime checkedAt;

    public HealthResponse(
            String status,
            String database,
            long companies,
            long people,
            long auditEvents,
            long tasks,
            LocalDateTime checkedAt) {
        this.status = status;
        this.database = database;
        this.companies = companies;
        this.people = people;
        this.auditEvents = auditEvents;
        this.tasks = tasks;
        this.checkedAt = checkedAt;
    }

    public String getStatus() {
        return status;
    }

    public String getDatabase() {
        return database;
    }

    public long getCompanies() {
        return companies;
    }

    public long getPeople() {
        return people;
    }

    public long getAuditEvents() {
        return auditEvents;
    }

    public long getTasks() {
        return tasks;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }
}
