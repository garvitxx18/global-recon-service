package global.recon.service.controller;

import global.recon.service.model.DiscoverMappingRequest;
import global.recon.service.model.ReconPlan;
import global.recon.service.model.UpdateReconPlanRequest;
import global.recon.service.service.MappingDiscoveryService;
import global.recon.service.service.ReconPlanService;
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

    private final MappingDiscoveryService mappingDiscoveryService;
    private final ReconPlanService reconPlanService;

    public ReconPlanController(MappingDiscoveryService mappingDiscoveryService, ReconPlanService reconPlanService) {
        this.mappingDiscoveryService = mappingDiscoveryService;
        this.reconPlanService = reconPlanService;
    }

    @PostMapping("/discover")
    @ResponseStatus(HttpStatus.CREATED)
    public ReconPlan discover(@Valid @RequestBody DiscoverMappingRequest request) {
        return mappingDiscoveryService.discover(request.getLeftDatasetId(), request.getRightDatasetId());
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
