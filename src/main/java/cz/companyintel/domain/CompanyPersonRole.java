package cz.companyintel.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class CompanyPersonRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Company company;

    @ManyToOne(optional = false)
    private Person person;

    @Column(nullable = false)
    private String role;

    protected CompanyPersonRole() {
    }

    public CompanyPersonRole(Company company, Person person, String role) {
        this.company = company;
        this.person = person;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public Person getPerson() {
        return person;
    }

    public String getRole() {
        return role;
    }

    public void updateRole(String role) {
        this.role = role;
    }
}
