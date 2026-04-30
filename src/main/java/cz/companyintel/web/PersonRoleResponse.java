package cz.companyintel.web;

import cz.companyintel.domain.CompanyPersonRole;

public class PersonRoleResponse {

    private Long personId;
    private String fullName;
    private String role;

    public static PersonRoleResponse from(CompanyPersonRole role) {
        PersonRoleResponse response = new PersonRoleResponse();
        response.personId = role.getPerson().getId();
        response.fullName = role.getPerson().getFullName();
        response.role = role.getRole();
        return response;
    }

    public Long getPersonId() {
        return personId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }
}
