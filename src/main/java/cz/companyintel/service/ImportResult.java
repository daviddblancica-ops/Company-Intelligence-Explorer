package cz.companyintel.service;

public class ImportResult {

    private final int imported;

    public ImportResult(int imported) {
        this.imported = imported;
    }

    public int getImported() {
        return imported;
    }
}
