package cz.companyintel.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class ImportRowError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ImportRun importRun;

    @Column(name = "source_row_number", nullable = false)
    private int rowNumber;

    @Column(length = 1200)
    private String rawValue;

    @Column(nullable = false, length = 800)
    private String message;

    protected ImportRowError() {
    }

    public ImportRowError(ImportRun importRun, int rowNumber, String rawValue, String message) {
        this.importRun = importRun;
        this.rowNumber = rowNumber;
        this.rawValue = rawValue;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getRawValue() {
        return rawValue;
    }

    public String getMessage() {
        return message;
    }
}
