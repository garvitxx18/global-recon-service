package global.recon.service.utils;

import global.recon.service.model.ColumnProfile;
import global.recon.service.model.DataType;
import global.recon.service.model.Dataset;
import global.recon.service.model.DatasetProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmPromptUtilityTest {

    private final LlmPromptUtility utility = new LlmPromptUtility();

    @Test
    void includesComparisonAndIngestNotes() {
        Dataset left = new Dataset();
        left.setRecordPath("tradeList");
        left.setIngestionNotes("Ignore header singles. Use tradeList only.");
        Dataset right = new Dataset();
        right.setRecordPath("tradeList");
        right.setIngestionNotes("Right book also has securityList.");
        String prompt = utility.buildPrompt(leftProfile(), rightProfile(), left, right,
                "Compare only the tradeList from each file.");
        assertThat(prompt).contains("USER COMPARISON NOTES:");
        assertThat(prompt).contains("Compare only the tradeList from each file.");
        assertThat(prompt).contains("Ingested JSON list path: tradeList");
        assertThat(prompt).contains("Ignore header singles. Use tradeList only.");
        assertThat(prompt).contains("Right book also has securityList.");
        assertThat(prompt).contains("trade_id");
    }

    private DatasetProfile leftProfile() {
        DatasetProfile profile = new DatasetProfile();
        profile.setDatasetId("left");
        ColumnProfile column = new ColumnProfile();
        column.setColumnName("trade_id");
        column.setType(DataType.STRING);
        column.setUniqueRatio(1.0);
        column.setNullPercentage(0);
        column.setSampleValues(List.of("T001"));
        profile.setColumns(List.of(column));
        return profile;
    }

    private DatasetProfile rightProfile() {
        DatasetProfile profile = new DatasetProfile();
        profile.setDatasetId("right");
        ColumnProfile column = new ColumnProfile();
        column.setColumnName("transaction_id");
        column.setType(DataType.STRING);
        column.setUniqueRatio(1.0);
        column.setNullPercentage(0);
        column.setSampleValues(List.of("T001"));
        profile.setColumns(List.of(column));
        return profile;
    }
}
