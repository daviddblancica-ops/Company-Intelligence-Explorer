package cz.companyintel.web;

import java.time.LocalDateTime;

public class DashboardResponse {

    private final long companies;
    private final long people;
    private final long relationships;
    private final long watchlisted;
    private final long auditEvents;
    private final long importRuns;
    private final LocalDateTime updatedAt;

    public DashboardResponse(
            long companies,
            long people,
            long relationships,
            long watchlisted,
            long auditEvents,
            long importRuns,
            LocalDateTime updatedAt) {
        this.companies = companies;
        this.people = people;
        this.relationships = relationships;
        this.watchlisted = watchlisted;
        this.auditEvents = auditEvents;
        this.importRuns = importRuns;
        this.updatedAt = updatedAt;
    }

    public long getCompanies() {
        return companies;
    }

    public long getPeople() {
        return people;
    }

    public long getRelationships() {
        return relationships;
    }

    public long getWatchlisted() {
        return watchlisted;
    }

    public long getAuditEvents() {
        return auditEvents;
    }

    public long getImportRuns() {
        return importRuns;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
