package global.recon.service.controller;

import global.recon.service.model.DiscoverMappingRequest;
import global.recon.service.model.ReconJob;
import global.recon.service.model.ReconPlan;
import global.recon.service.model.UpdateReconPlanRequest;
import global.recon.service.service.JobQueueService;
import global.recon.service.service.ReconPlanService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recon-plans")
public class ReconPlanController {

    private final JobQueueService jobQueueService;
    private final ReconPlanService reconPlanService;

    public ReconPlanController(JobQueueService jobQueueService, ReconPlanService reconPlanService) {
        this.jobQueueService = jobQueueService;
        this.reconPlanService = reconPlanService;
    }

    @Operation(summary = "Queue mapping discovery", description = "Returns a job immediately. Poll GET /api/v1/jobs/{jobId} until COMPLETED, then use resultId as the plan id.")
    @PostMapping("/discover")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ReconJob discover(@Valid @RequestBody DiscoverMappingRequest request) {
        return jobQueueService.enqueueDiscovery(
                request.getLeftDatasetId(), request.getRightDatasetId(), request.getNotes());
    }

    @GetMapping("/{planId}")
    public ReconPlan get(@PathVariable String planId) {
        return reconPlanService.getPlan(planId);
    }

    @PutMapping("/{planId}")
    public ReconPlan update(@PathVariable String planId, @RequestBody UpdateReconPlanRequest request) {
        return reconPlanService.updatePlan(planId, request);
    }

    @PostMapping("/{planId}/approve")
    public ReconPlan approve(@PathVariable String planId) {
        return reconPlanService.approve(planId);
    }
}
