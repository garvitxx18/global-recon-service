package global.recon.service.utils;

import global.recon.service.model.ColumnProfile;
import global.recon.service.model.DataType;
import global.recon.service.model.DatasetProfile;
import global.recon.service.model.LlmMappingResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmResponseUtilityTest {

    private final LlmResponseUtility utility = new LlmResponseUtility(JsonMapper.builder().build());

    @Test
    void parsesFencedJsonAndValidatesFields() {
        String raw = """
                ```json
                {
                  "keyMappings": [
                    {"leftField": "trade_id", "rightField": "transaction_id", "confidence": 0.99}
                  ],
                  "fieldMappings": [
                    {"leftField": "quantity", "rightField": "qty", "matchType": "EXACT", "confidence": 0.9}
                  ],
                  "overallConfidence": 0.98
                }
                ```
                """;
        LlmMappingResponse parsed = utility.parse(raw);
        LlmResponseUtility.ValidationResult result = utility.validate(parsed, leftProfile(), rightProfile());
        assertThat(result.response().getKeyMappings()).hasSize(1);
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void keepsDraftWhenKeysHallucinated() {
        LlmMappingResponse parsed = utility.parse("""
                {"keyMappings":[{"leftField":"missing","rightField":"transaction_id","confidence":0.9}],"fieldMappings":[],"overallConfidence":0.2}
                """);
        LlmResponseUtility.ValidationResult result = utility.validate(parsed, leftProfile(), rightProfile());
        assertThat(result.response().getKeyMappings()).isEmpty();
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("common field"));
    }

    private DatasetProfile leftProfile() {
        DatasetProfile profile = new DatasetProfile();
        profile.setColumns(List.of(column("trade_id"), column("quantity")));
        return profile;
    }

    private DatasetProfile rightProfile() {
        DatasetProfile profile = new DatasetProfile();
        profile.setColumns(List.of(column("transaction_id"), column("qty")));
        return profile;
    }

    private ColumnProfile column(String name) {
        ColumnProfile profile = new ColumnProfile();
        profile.setColumnName(name);
        profile.setType(DataType.STRING);
        return profile;
    }
}
