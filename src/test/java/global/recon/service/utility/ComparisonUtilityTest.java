package global.recon.service.utility;

import global.recon.service.model.MatchType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ComparisonUtilityTest {

    private final ComparisonUtility comparisonUtility =
            new ComparisonUtility(new DateNormalizationUtility(), new NumericUtility());

    @Test
    void exactAndCaseInsensitive() {
        assertThat(comparisonUtility.matches("T001", "T001", MatchType.EXACT, null)).isTrue();
        assertThat(comparisonUtility.matches("USD", "usd", MatchType.CASE_INSENSITIVE, null)).isTrue();
    }

    @Test
    void dateNormalized() {
        assertThat(comparisonUtility.matches("2026-08-20", "20/08/2026", MatchType.DATE_NORMALIZED, null)).isTrue();
    }

    @Test
    void numericTolerance() {
        assertThat(comparisonUtility.matches("220.50", "220.51", MatchType.NUMERIC_TOLERANCE, new BigDecimal("0.01"))).isTrue();
        assertThat(comparisonUtility.matches("220.50", "220.60", MatchType.NUMERIC_TOLERANCE, new BigDecimal("0.01"))).isFalse();
    }
}
