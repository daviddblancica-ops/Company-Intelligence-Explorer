package cz.companyintel.service;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class NormalizationService {

    public String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ascii
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
