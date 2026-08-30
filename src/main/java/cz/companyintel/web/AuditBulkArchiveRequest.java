package cz.companyintel.web;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public class AuditBulkArchiveRequest {

    @NotEmpty(message = "Je vyžadováno alespoň jedno ID auditní události")
    @Size(max = 500, message = "Požadavek může obsahovat nejvýše 500 ID auditních událostí")
    private List<@NotNull(message = "ID auditní události nesmí být prázdné")
            @Positive(message = "ID auditní události musí být kladné") Long> ids;
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
