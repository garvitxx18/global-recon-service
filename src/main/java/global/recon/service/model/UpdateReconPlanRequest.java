package global.recon.service.model;

import java.util.ArrayList;
import java.util.List;

public class UpdateReconPlanRequest {

    private List<ReconKeyMappingRequest> keyMappings = new ArrayList<>();
    private List<ReconFieldMappingRequest> fieldMappings = new ArrayList<>();

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
}
