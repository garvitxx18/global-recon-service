package global.recon.service.utils;

import global.recon.service.model.ColumnProfile;
import global.recon.service.model.Dataset;
import global.recon.service.model.DatasetProfile;
import org.springframework.stereotype.Component;

@Component
public class LlmPromptUtility {

    public String buildPrompt(DatasetProfile left, DatasetProfile right) {
        return buildPrompt(left, right, null, null, null);
    }

    public String buildPrompt(
            DatasetProfile left,
            DatasetProfile right,
            Dataset leftDataset,
            Dataset rightDataset,
            String comparisonNotes) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a reconciliation mapping assistant.\n\n");
        prompt.append("Determine the best way to reconcile these two datasets.\n\n");
        if (hasText(comparisonNotes)) {
            prompt.append("USER COMPARISON NOTES:\n").append(comparisonNotes.trim()).append("\n\n");
            prompt.append("Follow those notes strictly. If the user says to ignore header/single-entry fields, do not use them as keys or compare fields. ");
            prompt.append("If they name a list (for example tradeList), map only fields from the ingested records of that list.\n\n");
        }
        appendDataset(prompt, "DATASET A", left, leftDataset);
        prompt.append("\n");
        appendDataset(prompt, "DATASET B", right, rightDataset);
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

                Rules:
                - keyMappings is the join key. One pair is a simple key. 2-3 pairs is a composite key (order matters).
                - fieldMappings MUST include every remaining non-key field pair the user might compare.
                - Field names MUST match the datasets exactly. Nested objects are flattened with dots (a.c.d), so use those exact names.
                - INTEGER and DECIMAL fields MUST use matchType NUMERIC_TOLERANCE. Set tolerance to 0 for an exact numeric match; use a larger number when small differences are allowed.
                - A numeric break is only reported when abs(left - right) is greater than tolerance.
                - If unsure of the key, still return your best guess. The user can override it.
                """);
        return prompt.toString();
    }

    private void appendDataset(StringBuilder prompt, String title, DatasetProfile profile, Dataset dataset) {
        prompt.append(title).append("\n\n");
        if (dataset != null) {
            if (hasText(dataset.getRecordPath())) {
                prompt.append("Ingested JSON list path: ").append(dataset.getRecordPath().trim()).append('\n');
            }
            if (hasText(dataset.getIngestionNotes())) {
                prompt.append("User ingest notes:\n").append(dataset.getIngestionNotes().trim()).append("\n");
            }
            prompt.append('\n');
        }
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
