package global.recon.service.service;

import global.recon.service.model.LlmMappingResponse;
import global.recon.service.model.ReconPlan;
import global.recon.service.model.UpdateReconPlanRequest;

import java.util.List;

public interface ReconPlanService {

    ReconPlan createDraft(
            String leftDatasetId,
            String rightDatasetId,
            LlmMappingResponse mapping,
            List<String> warnings,
            String userNotes);

    ReconPlan createEmptyDraft(String leftDatasetId, String rightDatasetId);

    ReconPlan getPlan(String planId);

    ReconPlan updatePlan(String planId, UpdateReconPlanRequest request);

    ReconPlan approve(String planId);
}
