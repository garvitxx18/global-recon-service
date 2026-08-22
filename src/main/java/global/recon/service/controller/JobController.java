package global.recon.service.controller;

import global.recon.service.model.JobStatus;
import global.recon.service.model.ReconJob;
import global.recon.service.service.JobQueueService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobQueueService jobQueueService;

    public JobController(JobQueueService jobQueueService) {
        this.jobQueueService = jobQueueService;
    }

    @Operation(summary = "Get async job status")
    @GetMapping("/{jobId}")
    public ReconJob get(@PathVariable String jobId) {
        return jobQueueService.getJob(jobId);
    }

    @Operation(summary = "List async jobs")
    @GetMapping
    public Page<ReconJob> list(
            @RequestParam(value = "status", required = false) JobStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return jobQueueService.listJobs(status, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }
}
