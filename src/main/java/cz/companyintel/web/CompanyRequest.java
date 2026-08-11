package cz.companyintel.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CompanyRequest {

    private String name;
    private String registrationNumber;
    private String country;
    private String legalForm;
    private String address;
    private String dataSource;
    private String registryFileNumber;
    private LocalDate registryRegistrationDate;
    private LocalDate incorporationDate;
    private BigDecimal shareCapital;
    private String shareCapitalCurrency;
    private List<PersonRole> people;

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getRegistryFileNumber() {
        return registryFileNumber;
    }

    public void setRegistryFileNumber(String registryFileNumber) {
        this.registryFileNumber = registryFileNumber;
    }

    public LocalDate getRegistryRegistrationDate() {
        return registryRegistrationDate;
    }

    public void setRegistryRegistrationDate(LocalDate registryRegistrationDate) {
        this.registryRegistrationDate = registryRegistrationDate;
    }

    public LocalDate getIncorporationDate() {
        return incorporationDate;
    }

    public void setIncorporationDate(LocalDate incorporationDate) {
        this.incorporationDate = incorporationDate;
    }

    public BigDecimal getShareCapital() {
        return shareCapital;
    }

    public void setShareCapital(BigDecimal shareCapital) {
        this.shareCapital = shareCapital;
    }

    public String getShareCapitalCurrency() {
        return shareCapitalCurrency;
    }

    public void setShareCapitalCurrency(String shareCapitalCurrency) {
        this.shareCapitalCurrency = shareCapitalCurrency;
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
