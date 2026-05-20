package cz.companyintel.service;

public class ImportPreviewRow {

    private final int rowNumber;
    private final boolean valid;
    private final String name;
    private final String registrationNumber;
    private final String message;
    private final String rawValue;

    public ImportPreviewRow(int rowNumber, boolean valid, String name, String registrationNumber, String message, String rawValue) {
        this.rowNumber = rowNumber;
        this.valid = valid;
        this.name = name;
        this.registrationNumber = registrationNumber;
        this.message = message;
        this.rawValue = rawValue;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public boolean isValid() {
        return valid;
    }

    public String getName() {
        return name;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getMessage() {
        return message;
    }

    public String getRawValue() {
        return rawValue;
    }
}
