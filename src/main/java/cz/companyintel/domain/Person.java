package cz.companyintel.domain;

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

    protected Person() {
    }

    public Person(String fullName, String normalizedName) {
        this.fullName = fullName;
        this.normalizedName = normalizedName;
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
}
