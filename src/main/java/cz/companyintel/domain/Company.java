package cz.companyintel.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(indexes = {
        @Index(name = "idx_company_normalized_name", columnList = "normalizedName"),
        @Index(name = "idx_company_registration_number", columnList = "registrationNumber"),
        @Index(name = "idx_company_watchlisted", columnList = "watchlisted")
})
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String normalizedName;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    private String country;

    private String legalForm;

    @Column(length = 600)
    private String address;

    private String dataSource;

    @Column(nullable = false)
    private boolean watchlisted;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<CompanyPersonRole> people = new LinkedHashSet<CompanyPersonRole>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<ChangeEvent> changes = new LinkedHashSet<ChangeEvent>();

    protected Company() {
    }

    public Company(String name, String normalizedName, String registrationNumber, String country, String legalForm) {
        LocalDateTime now = LocalDateTime.now();
        this.name = name;
        this.normalizedName = normalizedName;
        this.registrationNumber = registrationNumber;
        this.country = country;
        this.legalForm = legalForm;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateProfile(String name, String normalizedName, String country, String legalForm) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.country = country;
        this.legalForm = legalForm;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String name, String normalizedName, String country, String legalForm, String address, String dataSource) {
        updateProfile(name, normalizedName, country, legalForm);
        this.address = address;
        this.dataSource = dataSource;
    }

    public void addRole(Person person, String role) {
        this.people.add(new CompanyPersonRole(this, person, role));
    }

    public void replaceRole(Person person, String role) {
        this.people.removeIf(existing -> existing.getPerson().getId() != null
                && existing.getPerson().getId().equals(person.getId()));
        addRole(person, role);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean updateRole(Long personId, String role) {
        for (CompanyPersonRole existing : this.people) {
            if (existing.getPerson().getId() != null && existing.getPerson().getId().equals(personId)) {
                existing.updateRole(role);
                this.updatedAt = LocalDateTime.now();
                return true;
            }
        }
        return false;
    }

    public boolean removeRole(Long personId) {
        boolean removed = this.people.removeIf(existing -> existing.getPerson().getId() != null
                && existing.getPerson().getId().equals(personId));
        if (removed) {
            this.updatedAt = LocalDateTime.now();
        }
        return removed;
    }

    public void clearRoles() {
        this.people.clear();
    }

    public void addChange(String type, String description) {
        this.changes.add(new ChangeEvent(this, type, description));
    }

    public void setWatchlisted(boolean watchlisted) {
        this.watchlisted = watchlisted;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getCountry() {
        return country;
    }

    public String getLegalForm() {
        return legalForm;
    }

    public String getAddress() {
        return address;
    }

    public String getDataSource() {
        return dataSource;
    }

    public boolean isWatchlisted() {
        return watchlisted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<CompanyPersonRole> getPeople() {
        return people;
    }

    public Set<ChangeEvent> getChanges() {
        return changes;
    }
}
