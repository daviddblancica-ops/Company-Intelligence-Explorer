package cz.companyintel.web;

import java.util.ArrayList;
import java.util.List;

public class CompanyRequest {

    private String name;
    private String registrationNumber;
    private String country;
    private String legalForm;
    private List<PersonRole> people = new ArrayList<PersonRole>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLegalForm() {
        return legalForm;
    }

    public void setLegalForm(String legalForm) {
        this.legalForm = legalForm;
    }

    public List<PersonRole> getPeople() {
        return people;
    }

    public void setPeople(List<PersonRole> people) {
        this.people = people;
    }

    public static class PersonRole {

        private String fullName;
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
}
