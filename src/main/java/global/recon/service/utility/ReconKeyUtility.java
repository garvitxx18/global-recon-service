package global.recon.service.utility;

import global.recon.service.model.ReconKeyMapping;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ReconKeyUtility {

    private final DateNormalizationUtility dateNormalizationUtility;
    private final NumericUtility numericUtility;

    public ReconKeyUtility(DateNormalizationUtility dateNormalizationUtility, NumericUtility numericUtility) {
        this.dateNormalizationUtility = dateNormalizationUtility;
        this.numericUtility = numericUtility;
    }

    public String buildKey(Map<String, Object> record, List<String> fields) {
        return fields.stream()
                .map(field -> normalizeValue(record.get(field)))
                .collect(Collectors.joining("|"));
    }

    public String buildLeftKey(Map<String, Object> record, List<ReconKeyMapping> mappings) {
        return mappings.stream()
                .map(mapping -> normalizeValue(record.get(mapping.getLeftField())))
                .collect(Collectors.joining("|"));
    }

    public String buildRightKey(Map<String, Object> record, List<ReconKeyMapping> mappings) {
        return mappings.stream()
                .map(mapping -> normalizeValue(record.get(mapping.getRightField())))
                .collect(Collectors.joining("|"));
    }

    public String normalizeValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Number) {
            String numeric = numericUtility.normalize(value);
            return numeric == null ? "" : numeric;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return "";
        }
        if (text.matches(".*[-/T].*") && dateNormalizationUtility.toLocalDate(text) != null) {
            return dateNormalizationUtility.normalize(text);
        }
        return text.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
