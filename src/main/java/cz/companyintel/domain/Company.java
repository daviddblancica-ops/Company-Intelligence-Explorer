package cz.companyintel.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompanyPersonRole> people = new ArrayList<CompanyPersonRole>();

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChangeEvent> changes = new ArrayList<ChangeEvent>();

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

    public void addRole(Person person, String role) {
        this.people.add(new CompanyPersonRole(this, person, role));
    }

    public void clearRoles() {
        this.people.clear();
    }

    public void addChange(String type, String description) {
        this.changes.add(new ChangeEvent(this, type, description));
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<CompanyPersonRole> getPeople() {
        return people;
    }

    public List<ChangeEvent> getChanges() {
        return changes;
    }
}
