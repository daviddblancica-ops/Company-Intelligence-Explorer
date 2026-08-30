package cz.companyintel.web;

import javax.validation.constraints.Size;

public class PersonRoleUpdateRequest {

    @Size(max = 255, message = "Role osoby může obsahovat nejvýše 255 znaků")
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
