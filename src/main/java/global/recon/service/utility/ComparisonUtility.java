package global.recon.service.utility;

import global.recon.service.model.MatchType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class ComparisonUtility {

    private final DateNormalizationUtility dateNormalizationUtility;
    private final NumericUtility numericUtility;

    public ComparisonUtility(DateNormalizationUtility dateNormalizationUtility, NumericUtility numericUtility) {
        this.dateNormalizationUtility = dateNormalizationUtility;
        this.numericUtility = numericUtility;
    }

    public boolean matches(Object left, Object right, MatchType matchType, BigDecimal tolerance) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return switch (matchType) {
            case EXACT -> exact(left, right);
            case CASE_INSENSITIVE -> String.valueOf(left).trim().equalsIgnoreCase(String.valueOf(right).trim());
            case DATE_NORMALIZED -> {
                LocalDate leftDate = dateNormalizationUtility.toLocalDate(left);
                LocalDate rightDate = dateNormalizationUtility.toLocalDate(right);
                yield leftDate != null && leftDate.equals(rightDate);
            }
            case NUMERIC_TOLERANCE -> {
                BigDecimal leftNumber = numericUtility.parse(left);
                BigDecimal rightNumber = numericUtility.parse(right);
                if (leftNumber == null || rightNumber == null) {
                    yield false;
                }
                BigDecimal allowed = tolerance == null ? BigDecimal.ZERO : tolerance.abs();
                yield leftNumber.subtract(rightNumber).abs().compareTo(allowed) <= 0;
            }
        };
    }

    private boolean exact(Object left, Object right) {
        BigDecimal leftNumber = numericUtility.parse(left);
        BigDecimal rightNumber = numericUtility.parse(right);
        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber) == 0;
        }
        return Objects.equals(String.valueOf(left).trim(), String.valueOf(right).trim());
    }
}
