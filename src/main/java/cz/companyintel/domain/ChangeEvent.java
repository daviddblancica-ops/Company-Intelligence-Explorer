package cz.companyintel.domain;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(indexes = {
        @Index(name = "idx_change_company_created", columnList = "company_id, createdAt"),
        @Index(name = "idx_change_type_created", columnList = "type, createdAt")
})
public class ChangeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 1200)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ChangeEvent() {
    }

    public ChangeEvent(Company company, String type, String description) {
        this.company = company;
        this.type = type;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
