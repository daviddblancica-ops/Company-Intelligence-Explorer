package cz.companyintel.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;

public class CompanyUpdateRequest {

    @NotBlank(message = "Název firmy je povinný")
    @Size(max = 255, message = "Název firmy může obsahovat nejvýše 255 znaků")
    private String name;

    @NotBlank(message = "IČO je povinné")
    @Size(max = 64, message = "Identifikace firmy může obsahovat nejvýše 64 znaků")
    private String registrationNumber;

    @Size(max = 3, message = "Kód země může obsahovat nejvýše 3 znaky")
    private String country;

    @Size(max = 255, message = "Právní forma může obsahovat nejvýše 255 znaků")
    private String legalForm;

    @Size(max = 600, message = "Adresa může obsahovat nejvýše 600 znaků")
    private String address;

    @Size(max = 255, message = "Zdroj dat může obsahovat nejvýše 255 znaků")
    private String dataSource;

    @Size(max = 120, message = "Spisová značka může obsahovat nejvýše 120 znaků")
    private String registryFileNumber;

    @PastOrPresent(message = "Datum zápisu nemůže být v budoucnosti")
    private LocalDate registryRegistrationDate;

    @PastOrPresent(message = "Datum vzniku nemůže být v budoucnosti")
    private LocalDate incorporationDate;

    @DecimalMin(value = "0.00", message = "Základní kapitál nesmí být záporný")
    @Digits(integer = 17, fraction = 2, message = "Základní kapitál může mít nejvýše 17 číslic a 2 desetinná místa")
    private BigDecimal shareCapital;

    @Size(max = 12, message = "Měna kapitálu může obsahovat nejvýše 12 znaků")
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
