package cz.companyintel.web;

import cz.companyintel.domain.CompanyPersonRole;
import cz.companyintel.domain.Person;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class PersonResponse {

    private Long id;
    private String fullName;
    private String normalizedName;
    private LocalDate dateOfBirth;
    private String residenceAddress;
    private String note;
    private boolean sensitiveDetailsVisible;
    private int companyCount;
    private int roleCount;
    private List<PersonCompanyResponse> companies;

    public static PersonResponse from(
            Person person,
            List<CompanyPersonRole> relationships,
            boolean includeSensitiveDetails) {
        PersonResponse response = new PersonResponse();
        response.id = person.getId();
        response.fullName = person.getFullName();
        response.normalizedName = person.getNormalizedName();
        response.sensitiveDetailsVisible = includeSensitiveDetails;
        if (includeSensitiveDetails) {
            response.dateOfBirth = person.getDateOfBirth();
            response.residenceAddress = person.getResidenceAddress();
            response.note = person.getNote();
        }
        response.roleCount = relationships.size();
        response.companyCount = (int) relationships.stream()
                .map(relationship -> relationship.getCompany().getId())
                .distinct()
                .count();
        response.companies = relationships.stream()
                .map(PersonCompanyResponse::from)
                .collect(Collectors.toList());
        return response;
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

    public boolean isSensitiveDetailsVisible() {
        return sensitiveDetailsVisible;
    }

    public int getCompanyCount() {
        return companyCount;
    }

    public int getRoleCount() {
        return roleCount;
    }

    public List<PersonCompanyResponse> getCompanies() {
        return companies;
    }
}
