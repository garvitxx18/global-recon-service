package global.recon.service.utils;

import global.recon.service.model.ColumnProfile;
import global.recon.service.model.DataType;
import global.recon.service.model.DatasetProfile;
import global.recon.service.model.LlmMappingResponse;
import global.recon.service.model.MatchType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MappingCompletionUtilityTest {

    private final MappingCompletionUtility utility = new MappingCompletionUtility();

    @Test
    void fillsRemainingCompareFieldsAndNumericThreshold() {
        DatasetProfile left = profile(
                column("Security_Code", DataType.STRING, 0.99),
                column("Units", DataType.INTEGER, 0.4),
                column("TradeDate", DataType.DATE, 0.3)
        );
        DatasetProfile right = profile(
                column("isin", DataType.STRING, 0.99),
                column("quantity", DataType.DECIMAL, 0.4),
                column("businessDate", DataType.DATE, 0.3)
        );
        LlmMappingResponse response = new LlmMappingResponse();
        LlmMappingResponse.LlmKeyMapping key = new LlmMappingResponse.LlmKeyMapping();
        key.setLeftField("Security_Code");
        key.setRightField("isin");
        response.getKeyMappings().add(key);

        utility.complete(response, left, right);

        assertThat(response.getFieldMappings()).hasSize(2);
        assertThat(response.getFieldMappings())
                .noneMatch(mapping -> "Security_Code".equals(mapping.getLeftField()));
        LlmMappingResponse.LlmFieldMapping units = response.getFieldMappings().stream()
                .filter(mapping -> "Units".equals(mapping.getLeftField()))
                .findFirst()
                .orElseThrow();
        assertThat(units.getRightField()).isEqualTo("quantity");
        assertThat(units.getMatchType()).isEqualTo(MatchType.NUMERIC_TOLERANCE.name());
        assertThat(units.getTolerance()).isEqualTo(0.0);
    }

    @Test
    void pairsAllFieldsWhenLlmReturnsNothing() {
        DatasetProfile left = profile(column("Txn_Ref", DataType.STRING, 1.0), column("Side", DataType.STRING, 0.1));
        DatasetProfile right = profile(column("transactionReference", DataType.STRING, 1.0), column("direction", DataType.STRING, 0.1));
        LlmMappingResponse response = new LlmMappingResponse();
        utility.complete(response, left, right);
        assertThat(response.getFieldMappings()).hasSize(2);
    }

    private DatasetProfile profile(ColumnProfile... columns) {
        DatasetProfile profile = new DatasetProfile();
        profile.setColumns(List.of(columns));
        return profile;
    }

    private ColumnProfile column(String name, DataType type, double uniqueRatio) {
        ColumnProfile profile = new ColumnProfile();
        profile.setColumnName(name);
        profile.setType(type);
        profile.setUniqueRatio(uniqueRatio);
        return profile;
    }
}
