package global.recon.service.controller;

import global.recon.service.model.CreateReconRunRequest;
import global.recon.service.model.ReconJob;
import global.recon.service.model.ReconResult;
import global.recon.service.model.ReconRun;
import global.recon.service.model.ReconStatus;
import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.JobQueueService;
import global.recon.service.service.ReconResultService;
import global.recon.service.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
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

    @Operation(summary = "Download a recon report as CSV", description = "Defaults to BREAK rows, one CSV line per field difference. Opens in Excel.")
    @GetMapping(value = "/{runId}/export", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> export(
            @PathVariable String runId,
            @RequestParam(value = "status", defaultValue = "BREAK") String statusParam) {
        reconciliationService.getRun(runId);
        ReconStatus status = parseExportStatus(statusParam);
        String filename = exportFilename(runId, status);
        StreamingResponseBody body = output -> reconResultService.writeCsvExport(runId, status, output);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    private ReconStatus parseExportStatus(String statusParam) {
        if (statusParam == null || statusParam.isBlank() || "ALL".equalsIgnoreCase(statusParam)) {
            return null;
        }
        try {
            return ReconStatus.valueOf(statusParam.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Unknown export status: " + statusParam);
        }
    }

    private String exportFilename(String runId, ReconStatus status) {
        String safeId = runId.replaceAll("[^A-Za-z0-9._-]", "_");
        if (status == null) {
            return "recon-report-" + safeId + ".csv";
        }
        if (status == ReconStatus.BREAK) {
            return "break-report-" + safeId + ".csv";
        }
        return "recon-report-" + safeId + "-" + status.name().toLowerCase(Locale.ROOT) + ".csv";
    }
}
