package cz.companyintel.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CompanyUpdateRequest {

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
}
