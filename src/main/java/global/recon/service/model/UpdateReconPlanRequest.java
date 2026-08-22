package global.recon.service.model;

import java.util.ArrayList;
import java.util.List;

public class UpdateReconPlanRequest {

    private List<ReconKeyMappingRequest> keyMappings = new ArrayList<>();
    private List<ReconFieldMappingRequest> fieldMappings = new ArrayList<>();
    private String leftDatasetId;
    private String rightDatasetId;
    private String userNotes;
    private Boolean approve;

    public List<ReconKeyMappingRequest> getKeyMappings() {
        return keyMappings;
    }

    public void setKeyMappings(List<ReconKeyMappingRequest> keyMappings) {
        this.keyMappings = keyMappings;
    }

    public List<ReconFieldMappingRequest> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<ReconFieldMappingRequest> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

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

    public String getUserNotes() {
        return userNotes;
    }

    public void setUserNotes(String userNotes) {
        this.userNotes = userNotes;
    }

    public Boolean getApprove() {
        return approve;
    }

    public void setApprove(Boolean approve) {
        this.approve = approve;
    }
}
