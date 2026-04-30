package cz.companyintel.web;

import cz.companyintel.domain.Company;
import java.util.List;
import java.util.stream.Collectors;

public class CompanyResponse {

    private Long id;
    private String name;
    private String normalizedName;
    private String registrationNumber;
    private String country;
    private String legalForm;
    private String address;
    private String dataSource;
    private List<PersonRoleResponse> people;
    private List<ChangeEventResponse> changes;

    public static CompanyResponse from(Company company) {
        CompanyResponse response = new CompanyResponse();
        response.id = company.getId();
        response.name = company.getName();
        response.normalizedName = company.getNormalizedName();
        response.registrationNumber = company.getRegistrationNumber();
        response.country = company.getCountry();
        response.legalForm = company.getLegalForm();
        response.address = company.getAddress();
        response.dataSource = company.getDataSource();
        response.people = company.getPeople().stream()
                .map(PersonRoleResponse::from)
                .collect(Collectors.toList());
        response.changes = company.getChanges().stream()
                .map(ChangeEventResponse::from)
                .collect(Collectors.toList());
        return response;
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

    public List<PersonRoleResponse> getPeople() {
        return people;
    }

    public List<ChangeEventResponse> getChanges() {
        return changes;
    }
}
