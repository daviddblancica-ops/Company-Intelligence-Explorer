package cz.companyintel.web;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;

public class PersonUpdateRequest {

    @NotBlank(message = "Jméno osoby je povinné")
    @Size(max = 255, message = "Jméno osoby může obsahovat nejvýše 255 znaků")
    private String fullName;

    @PastOrPresent(message = "Datum narození nemůže být v budoucnosti")
    private LocalDate dateOfBirth;

    @Size(max = 600, message = "Bydliště může obsahovat nejvýše 600 znaků")
    private String residenceAddress;

    @Size(max = 1200, message = "Poznámka může obsahovat nejvýše 1200 znaků")
    private String note;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getResidenceAddress() {
        return residenceAddress;
    }

    public void setResidenceAddress(String residenceAddress) {
        this.residenceAddress = residenceAddress;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
