package cz.companyintel.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AuditFilter {

    private static final Set<String> SEVERITIES = new HashSet<String>(
            Arrays.asList("LOW", "INFO", "WARNING", "CRITICAL"));

    private final String type;
    private final String severity;
    private final boolean archived;
    private final Long companyId;
    private final Long importRunId;
    private final String query;
    private final LocalDate from;
    private final LocalDate to;
    private final int limit;

    public AuditFilter(
            String type,
            String severity,
            boolean archived,
            Long companyId,
            Long importRunId,
            String query,
            LocalDate from,
            LocalDate to,
            int limit) {
        this.type = normalizeUppercase(type);
        this.severity = normalizeSeverity(severity);
        this.archived = archived;
        this.companyId = positiveId(companyId, "Company id");
        this.importRunId = positiveId(importRunId, "Import run id");
        this.query = normalize(query);
        this.from = from;
        this.to = to;
        this.limit = Math.max(1, Math.min(limit, 5000));
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Audit date range is invalid");
        }
    }

    public String getType() {
        return type;
    }

    public String getSeverity() {
        return severity;
    }

    public boolean isArchived() {
        return archived;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getImportRunId() {
        return importRunId;
    }

    public String getQuery() {
        return query;
    }

    public LocalDateTime getFromDateTime() {
        return from == null ? null : from.atStartOfDay();
    }

    public LocalDateTime getToExclusiveDateTime() {
        return to == null ? null : to.plusDays(1).atStartOfDay();
    }

    public int getLimit() {
        return limit;
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeUppercase(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private static String normalizeSeverity(String value) {
        String normalized = normalizeUppercase(value);
        if (normalized != null && !SEVERITIES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported audit severity: " + value);
        }
        return normalized;
    }

    private static Long positiveId(Long value, String label) {
        if (value != null && value.longValue() < 1L) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return value;
    }
}
