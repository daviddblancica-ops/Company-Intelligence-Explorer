package cz.companyintel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImportLimits {

    private static final int MAX_CONFIGURED_PAYLOAD_BYTES = 50 * 1024 * 1024;
    private static final int MAX_CONFIGURED_ROWS = 100000;
    private static final int MAX_CONFIGURED_PEOPLE = 10000;

    private final int maxPayloadBytes;
    private final int maxRows;
    private final int maxPeoplePerCompany;

    public ImportLimits(
            @Value("${app.import.max-payload-bytes:1048576}") int maxPayloadBytes,
            @Value("${app.import.max-rows:1000}") int maxRows,
            @Value("${app.import.max-people-per-company:100}") int maxPeoplePerCompany) {
        this.maxPayloadBytes = bounded(
                maxPayloadBytes, "app.import.max-payload-bytes", MAX_CONFIGURED_PAYLOAD_BYTES);
        this.maxRows = bounded(maxRows, "app.import.max-rows", MAX_CONFIGURED_ROWS);
        this.maxPeoplePerCompany = bounded(
                maxPeoplePerCompany, "app.import.max-people-per-company", MAX_CONFIGURED_PEOPLE);
    }

    public int getMaxPayloadBytes() {
        return maxPayloadBytes;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public int getMaxPeoplePerCompany() {
        return maxPeoplePerCompany;
    }

    private int bounded(int value, String property, int maximum) {
        if (value < 1 || value > maximum) {
            throw new IllegalStateException(
                    property + " musí být celé číslo v rozsahu 1 až " + maximum + ".");
        }
        return value;
    }
}
