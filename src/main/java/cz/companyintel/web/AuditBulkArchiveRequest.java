package cz.companyintel.web;

import java.util.List;

public class AuditBulkArchiveRequest {

    private List<Long> ids;
    private boolean archived;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
