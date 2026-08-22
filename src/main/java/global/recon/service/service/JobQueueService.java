package global.recon.service.service;

import global.recon.service.model.JobStatus;
import global.recon.service.model.ReconJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobQueueService {

    ReconJob enqueueDiscovery(String leftDatasetId, String rightDatasetId, String notes);

    ReconJob enqueueReconRun(String reconPlanId);

    ReconJob getJob(String jobId);

    Page<ReconJob> listJobs(JobStatus status, Pageable pageable);

    void process(String jobId);

    void recoverQueuedJobs();
}
