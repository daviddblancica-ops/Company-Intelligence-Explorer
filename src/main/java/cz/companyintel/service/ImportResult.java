package cz.companyintel.service;

public class ImportResult {

    private final int imported;
    private final int failed;
    private final Long runId;
    private final String status;

    public ImportResult(int imported, int failed, Long runId, String status) {
        this.imported = imported;
        this.failed = failed;
        this.runId = runId;
        this.status = status;
    }

    public int getImported() {
        return imported;
    }

    public int getFailed() {
        return failed;
    }

    public Long getRunId() {
        return runId;
    }

    public String getStatus() {
        return status;
    }
}
