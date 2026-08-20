package global.recon.service.model;

public class ReconDifference {

    private String leftField;
    private String rightField;
    private String leftValue;
    private String rightValue;

    public ReconDifference() {
    }

    public ReconDifference(String leftField, String rightField, String leftValue, String rightValue) {
        this.leftField = leftField;
        this.rightField = rightField;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }

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

    public String getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(String leftValue) {
        this.leftValue = leftValue;
    }

    public String getRightValue() {
        return rightValue;
    }

    public void setRightValue(String rightValue) {
        this.rightValue = rightValue;
    }
}
