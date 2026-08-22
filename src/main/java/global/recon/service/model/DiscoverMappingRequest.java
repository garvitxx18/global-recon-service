package global.recon.service.model;

public class DiscoverMappingRequest {

    private String leftDatasetId;
    private String rightDatasetId;
    private String notes;

    public String getLeftDatasetId() {
        return leftDatasetId;
    }

    public void setLeftDatasetId(String leftDatasetId) {
        this.leftDatasetId = leftDatasetId;
    }

    public String getRightDatasetId() {
        return rightDatasetId;
    }

    public void setRightDatasetId(String rightDatasetId) {
        this.rightDatasetId = rightDatasetId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
