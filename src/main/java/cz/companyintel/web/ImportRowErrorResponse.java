package cz.companyintel.web;

import cz.companyintel.domain.ImportRowError;

public class ImportRowErrorResponse {

    private int rowNumber;
    private String rawValue;
    private String message;

    public static ImportRowErrorResponse from(ImportRowError error) {
        ImportRowErrorResponse response = new ImportRowErrorResponse();
        response.rowNumber = error.getRowNumber();
        response.rawValue = error.getRawValue();
        response.message = error.getMessage();
        return response;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getRawValue() {
        return rawValue;
    }

    public String getMessage() {
        return message;
    }
}
