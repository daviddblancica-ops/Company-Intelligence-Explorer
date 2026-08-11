package cz.companyintel.domain;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(indexes = {
        @Index(name = "idx_person_normalized_name", columnList = "normalizedName")
})
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String normalizedName;

    private LocalDate dateOfBirth;

    @Column(length = 600)
    private String residenceAddress;

    @Column(length = 1200)
    private String note;

    protected Person() {
    }

    public Person(String fullName, String normalizedName) {
        this.fullName = fullName;
        this.normalizedName = normalizedName;
    }

    public void updateProfile(
            String fullName,
            String normalizedName,
            LocalDate dateOfBirth,
            String residenceAddress,
            String note) {
        this.fullName = fullName;
        this.normalizedName = normalizedName;
        this.dateOfBirth = dateOfBirth;
        this.residenceAddress = residenceAddress;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getResidenceAddress() {
        return residenceAddress;
    }

    public String getNote() {
        return note;
    }
}
