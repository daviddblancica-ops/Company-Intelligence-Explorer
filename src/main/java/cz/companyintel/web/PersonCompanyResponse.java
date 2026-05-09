package cz.companyintel.web;

import cz.companyintel.domain.Company;
import cz.companyintel.domain.CompanyPersonRole;

public class PersonCompanyResponse {

    private Long companyId;
    private String companyName;
    private String registrationNumber;
    private String role;
    private boolean watchlisted;

    public static PersonCompanyResponse from(CompanyPersonRole relationship) {
        Company company = relationship.getCompany();
        PersonCompanyResponse response = new PersonCompanyResponse();
        response.companyId = company.getId();
        response.companyName = company.getName();
        response.registrationNumber = company.getRegistrationNumber();
        response.role = relationship.getRole();
        response.watchlisted = company.isWatchlisted();
        return response;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getRole() {
        return role;
    }

    public boolean isWatchlisted() {
        return watchlisted;
    }
}
