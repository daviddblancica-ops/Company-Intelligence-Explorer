package cz.companyintel.service;

import java.util.ArrayList;
import java.util.List;

public class ImportPreview {

    private final String sourceType;
    private final int totalRows;
    private final int validRows;
    private final int invalidRows;
    private final List<ImportPreviewRow> rows;

    public ImportPreview(String sourceType, int totalRows, int validRows, int invalidRows, List<ImportPreviewRow> rows) {
        this.sourceType = sourceType;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.rows = new ArrayList<ImportPreviewRow>(rows);
    }

    public String getSourceType() {
        return sourceType;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getValidRows() {
        return validRows;
    }

    public int getInvalidRows() {
        return invalidRows;
    }

    public List<ImportPreviewRow> getRows() {
        return rows;
    }
}
