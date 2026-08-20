package global.recon.service.model;

public class ReconKeyMappingRequest {

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
