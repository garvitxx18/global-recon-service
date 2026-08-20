package global.recon.service.model;

import java.util.ArrayList;
import java.util.List;

public class LlmMappingResponse {

    private List<LlmKeyMapping> keyMappings = new ArrayList<>();
    private List<LlmFieldMapping> fieldMappings = new ArrayList<>();
    private Double overallConfidence;

    public List<LlmKeyMapping> getKeyMappings() {
        return keyMappings;
    }

    public void setKeyMappings(List<LlmKeyMapping> keyMappings) {
        this.keyMappings = keyMappings;
    }

    public List<LlmFieldMapping> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<LlmFieldMapping> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    public Double getOverallConfidence() {
        return overallConfidence;
    }

    public void setOverallConfidence(Double overallConfidence) {
        this.overallConfidence = overallConfidence;
    }

    public static class LlmKeyMapping {
        private String leftField;
        private String rightField;
        private Double confidence;

        public String getLeftField() {
            return leftField;
        }

        public void setLeftField(String leftField) {
            this.leftField = leftField;
        }

        public String getRightField() {
            return rightField;
        }

        public void setRightField(String rightField) {
            this.rightField = rightField;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }
    }

    public static class LlmFieldMapping {
        private String leftField;
        private String rightField;
        private String matchType;
        private Double confidence;
        private Double tolerance;

        public String getLeftField() {
            return leftField;
        }

        public void setLeftField(String leftField) {
            this.leftField = leftField;
        }

        public String getRightField() {
            return rightField;
        }

        public void setRightField(String rightField) {
            this.rightField = rightField;
        }

        public String getMatchType() {
            return matchType;
        }

        public void setMatchType(String matchType) {
            this.matchType = matchType;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public Double getTolerance() {
            return tolerance;
        }

        public void setTolerance(Double tolerance) {
            this.tolerance = tolerance;
        }
    }
}
