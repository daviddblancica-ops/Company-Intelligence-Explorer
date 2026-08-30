package cz.companyintel.service;

import cz.companyintel.domain.ChangeEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditCsvExporter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public String export(List<ChangeEvent> events) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("čas,důležitost,typ,subjekt,IČO,ID importu,popis,archivováno\r\n");
        for (ChangeEvent event : events) {
            append(csv, event.getCreatedAt() == null ? "" : DATE_FORMAT.format(event.getCreatedAt()));
            append(csv, event.getSeverity());
            append(csv, event.getType());
            append(csv, subject(event));
            append(csv, registrationNumber(event));
            append(csv, event.getImportRun() == null ? "" : String.valueOf(event.getImportRun().getId()));
            append(csv, event.getDescription());
            appendLast(csv, String.valueOf(event.isArchived()));
        }
        return csv.toString();
    }

    private static String subject(ChangeEvent event) {
        if (event.getImportRun() != null) {
            return "Import #" + event.getImportRun().getId();
        }
        if (event.getCompanyName() != null && !event.getCompanyName().trim().isEmpty()) {
            return event.getCompanyName();
        }
        return event.getCompany() == null ? "Systém" : event.getCompany().getName();
    }

    private static String registrationNumber(ChangeEvent event) {
        if (event.getRegistrationNumber() != null && !event.getRegistrationNumber().trim().isEmpty()) {
            return event.getRegistrationNumber();
        }
        return event.getCompany() == null ? "" : event.getCompany().getRegistrationNumber();
    }

    private static void append(StringBuilder csv, String value) {
        csv.append(escape(value)).append(',');
    }

    private static void appendLast(StringBuilder csv, String value) {
        csv.append(escape(value)).append("\r\n");
    }

    private static String escape(String value) {
        String safeValue = neutralizeSpreadsheetFormula(value == null ? "" : value);
        return '"' + safeValue.replace("\"", "\"\"") + '"';
    }

    private static String neutralizeSpreadsheetFormula(String value) {
        if (value.isEmpty()) {
            return value;
        }
        if (value.charAt(0) == '\t' || value.charAt(0) == '\r') {
            return "'" + value;
        }
        int firstContent = 0;
        while (firstContent < value.length() && Character.isWhitespace(value.charAt(firstContent))) {
            firstContent++;
        }
        if (firstContent < value.length()) {
            char first = value.charAt(firstContent);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                return "'" + value;
            }
        }
        return value;
    }
}
