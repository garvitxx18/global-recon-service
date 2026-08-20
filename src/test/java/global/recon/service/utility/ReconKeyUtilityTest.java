package global.recon.service.utility;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReconKeyUtilityTest {

    private final ReconKeyUtility reconKeyUtility = new ReconKeyUtility(new DateNormalizationUtility(), new NumericUtility());

    @Test
    void buildsSimpleKey() {
        String key = reconKeyUtility.buildKey(Map.of("trade_id", "T001"), List.of("trade_id"));
        assertThat(key).isEqualTo("T001");
    }

    @Test
    void buildsCompositeKey() {
        String key = reconKeyUtility.buildKey(
                Map.of("security_id", "US0378331005", "trade_date", "2026-08-20"),
                List.of("security_id", "trade_date"));
        assertThat(key).isEqualTo("US0378331005|2026-08-20");
    }

    @Test
    void normalizesCaseAndWhitespace() {
        assertThat(reconKeyUtility.normalizeValue(" t001 ")).isEqualTo("T001");
    }
}
