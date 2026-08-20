package global.recon.service.utility;

import global.recon.service.model.ColumnProfile;
import global.recon.service.model.DatasetProfile;
import org.springframework.stereotype.Component;

@Component
public class LlmPromptUtility {

    public String buildPrompt(DatasetProfile left, DatasetProfile right) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a reconciliation mapping assistant.\n\n");
        prompt.append("Determine the best way to reconcile these two datasets.\n\n");
        appendDataset(prompt, "DATASET A", left);
        prompt.append("\n");
        appendDataset(prompt, "DATASET B", right);
        prompt.append("""

                Return:
                1. Best reconciliation key
                2. Candidate composite key if needed
                3. Field mappings
                4. Comparison type
                5. Required transformations
                6. Confidence score

                Return ONLY valid JSON with this schema:
                {
                  "keyMappings": [
                    { "leftField": "string", "rightField": "string", "confidence": 0.0 }
                  ],
                  "fieldMappings": [
                    { "leftField": "string", "rightField": "string", "matchType": "EXACT|CASE_INSENSITIVE|DATE_NORMALIZED|NUMERIC_TOLERANCE", "confidence": 0.0, "tolerance": 0.01 }
                  ],
                  "overallConfidence": 0.0
                }
                """);
        return prompt.toString();
    }

    private void appendDataset(StringBuilder prompt, String title, DatasetProfile profile) {
        prompt.append(title).append("\n\n");
        for (ColumnProfile column : profile.getColumns()) {
            prompt.append("Field:\n").append(column.getColumnName()).append('\n');
            prompt.append("Type:\n").append(column.getType()).append('\n');
            prompt.append("Unique Ratio:\n").append(column.getUniqueRatio()).append('\n');
            prompt.append("Null Percentage:\n").append(column.getNullPercentage()).append('\n');
            prompt.append("Samples:\n");
            if (column.getSampleValues() == null || column.getSampleValues().isEmpty()) {
                prompt.append("(none)\n");
            } else {
                prompt.append(String.join(", ", column.getSampleValues())).append('\n');
            }
            prompt.append('\n');
        }
    }
}
