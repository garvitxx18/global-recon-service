package global.recon.service.utils;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NumericUtility {

    public BigDecimal parse(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = String.valueOf(value).trim().replace(",", "");
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public String normalize(Object value) {
        BigDecimal parsed = parse(value);
        return parsed == null ? null : parsed.stripTrailingZeros().toPlainString();
    }
}
