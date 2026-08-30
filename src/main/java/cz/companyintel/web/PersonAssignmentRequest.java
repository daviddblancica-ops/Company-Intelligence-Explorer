package cz.companyintel.web;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PersonAssignmentRequest {

    @NotBlank(message = "Jméno osoby je povinné")
    @Size(max = 255, message = "Jméno osoby může obsahovat nejvýše 255 znaků")
    private String fullName;

    @Size(max = 255, message = "Role osoby může obsahovat nejvýše 255 znaků")
    private String role;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
