package global.recon.service.controller;

import global.recon.service.model.CreateReconRunRequest;
import global.recon.service.model.ReconJob;
import global.recon.service.model.ReconResult;
import global.recon.service.model.ReconRun;
import global.recon.service.model.ReconStatus;
import global.recon.service.service.JobQueueService;
import global.recon.service.service.ReconResultService;
import global.recon.service.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/recon-runs")
public class ReconRunController {

    private final ReconciliationService reconciliationService;
    private final ReconResultService reconResultService;
    private final JobQueueService jobQueueService;

    public ReconRunController(
            ReconciliationService reconciliationService,
            ReconResultService reconResultService,
            JobQueueService jobQueueService) {
        this.reconciliationService = reconciliationService;
        this.reconResultService = reconResultService;
        this.jobQueueService = jobQueueService;
    }

    @Operation(summary = "Queue a reconciliation run", description = "Returns a job immediately. Poll GET /api/v1/jobs/{jobId} until COMPLETED, then use resultId as the run id.")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ReconJob create(@RequestBody CreateReconRunRequest request) {
        return jobQueueService.enqueueReconRun(request.getReconPlanId());
    }

    @GetMapping("/{runId}")
    public ReconRun get(@PathVariable String runId) {
        return reconciliationService.getRun(runId);
    }

    @GetMapping("/{runId}/summary")
    public Map<String, Long> summary(@PathVariable String runId) {
        ReconRun run = reconciliationService.getRun(runId);
        return reconResultService.summarize(run);
    }

    @GetMapping("/{runId}/results")
    public Page<ReconResult> results(
            @PathVariable String runId,
            @RequestParam(value = "status", required = false) ReconStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        reconciliationService.getRun(runId);
        return reconResultService.getResults(
                runId,
                status,
                PageRequest.of(page, size, Sort.by("id").ascending()));
    }
}
