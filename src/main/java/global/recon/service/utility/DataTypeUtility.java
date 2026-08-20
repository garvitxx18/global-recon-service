package global.recon.service.utility;

import global.recon.service.model.DataType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Component
public class DataTypeUtility {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
    );

    private static final List<DateTimeFormatter> DATETIME_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    );

    public DataType detect(Collection<Object> samples) {
        boolean sawBoolean = false;
        boolean sawInteger = false;
        boolean sawDecimal = false;
        boolean sawDate = false;
        boolean sawDateTime = false;
        boolean sawString = false;
        int nonNull = 0;

        for (Object sample : samples) {
            if (sample == null || String.valueOf(sample).isBlank()) {
                continue;
            }
            nonNull++;
            String text = String.valueOf(sample).trim();
            if (isBoolean(text)) {
                sawBoolean = true;
            } else if (isInteger(text)) {
                sawInteger = true;
            } else if (isDecimal(text)) {
                sawDecimal = true;
            } else if (isDateTime(text)) {
                sawDateTime = true;
            } else if (isDate(text)) {
                sawDate = true;
            } else {
                sawString = true;
            }
        }

        if (nonNull == 0 || sawString) {
            return DataType.STRING;
        }
        if (sawBoolean && !sawInteger && !sawDecimal && !sawDate && !sawDateTime) {
            return DataType.BOOLEAN;
        }
        if (sawDateTime) {
            return DataType.DATETIME;
        }
        if (sawDate && !sawInteger && !sawDecimal) {
            return DataType.DATE;
        }
        if (sawDecimal) {
            return DataType.DECIMAL;
        }
        if (sawInteger) {
            return DataType.INTEGER;
        }
        return DataType.STRING;
    }

    public boolean isBoolean(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "false".equals(normalized) || "yes".equals(normalized) || "no".equals(normalized);
    }

    public boolean isInteger(String value) {
        try {
            Long.parseLong(value.replace(",", ""));
            return !value.contains(".");
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public boolean isDecimal(String value) {
        try {
            new java.math.BigDecimal(value.replace(",", ""));
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public boolean isDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                LocalDate.parse(value, formatter);
                return true;
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return false;
    }

    public boolean isDateTime(String value) {
        for (DateTimeFormatter formatter : DATETIME_FORMATS) {
            try {
                LocalDateTime.parse(value, formatter);
                return true;
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return false;
    }
}
