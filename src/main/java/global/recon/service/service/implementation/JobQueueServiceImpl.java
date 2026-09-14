package global.recon.service.service.implementation;

import global.recon.service.config.HazelcastConfig;
import global.recon.service.config.OwnerAccess;
import global.recon.service.config.UserContext;
import global.recon.service.model.JobStatus;
import global.recon.service.model.JobType;
import global.recon.service.model.ReconJob;
import global.recon.service.model.ReconPlan;
import global.recon.service.model.ReconRun;
import global.recon.service.repository.ReconJobRepository;
import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.JobQueueService;
import global.recon.service.service.MappingDiscoveryService;
import global.recon.service.service.ReconciliationService;
import global.recon.service.service.ResourceNotFoundException;
import global.recon.service.utils.IdUtility;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class JobQueueServiceImpl implements JobQueueService {

    private static final Logger log = LoggerFactory.getLogger(JobQueueServiceImpl.class);

    private final ReconJobRepository reconJobRepository;
    private final MappingDiscoveryService mappingDiscoveryService;
    private final ReconciliationService reconciliationService;
    private final HazelcastInstance hazelcastInstance;

    public JobQueueServiceImpl(
            ReconJobRepository reconJobRepository,
            MappingDiscoveryService mappingDiscoveryService,
            ReconciliationService reconciliationService,
            HazelcastInstance hazelcastInstance) {
        this.reconJobRepository = reconJobRepository;
        this.mappingDiscoveryService = mappingDiscoveryService;
        this.reconciliationService = reconciliationService;
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public ReconJob enqueueDiscovery(String leftDatasetId, String rightDatasetId, String notes) {
        ReconJob job = newJob(JobType.MAPPING_DISCOVERY);
        job.setLeftDatasetId(leftDatasetId);
        job.setRightDatasetId(rightDatasetId);
        String trimmed = notes == null ? null : notes.trim();
        if (trimmed != null && trimmed.isEmpty()) {
            trimmed = null;
        }
        if (trimmed != null && trimmed.length() > 4000) {
            throw new InvalidRequestException("Comparison notes must be 4000 characters or fewer");
        }
        job.setNotes(trimmed);
        reconJobRepository.save(job);
        return runNow(job.getId());
    }

    @Override
    public ReconJob enqueueReconRun(String reconPlanId) {
        ReconRun run = reconciliationService.submit(reconPlanId);
        ReconJob job = newJob(JobType.RECON_RUN);
        job.setReconPlanId(reconPlanId);
        job.setResultId(run.getId());
        reconJobRepository.save(job);
        return runNow(job.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ReconJob getJob(String jobId) {
        ReconJob job = reconJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        OwnerAccess.assertOwns(job.getOwnerEmail());
        return job;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReconJob> listJobs(JobStatus status, Pageable pageable) {
        String email = UserContext.require();
        if (status == null) {
            return reconJobRepository.findByOwnerEmail(email, pageable);
        }
        return reconJobRepository.findByOwnerEmailAndStatus(email, status, pageable);
    }

    @Override
    @Transactional
    public void process(String jobId) {
        ReconJob job = reconJobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() == JobStatus.COMPLETED) {
            return;
        }
        if (job.getOwnerEmail() == null || job.getOwnerEmail().isBlank()) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage("Job is missing owner email");
            job.setCompletedAt(Instant.now());
            reconJobRepository.save(job);
            return;
        }
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        reconJobRepository.save(job);
        String previousEmail = UserContext.get();
        try {
            UserContext.set(job.getOwnerEmail());
            if (job.getType() == JobType.MAPPING_DISCOVERY) {
                ReconPlan plan = mappingDiscoveryService.discover(
                        job.getLeftDatasetId(), job.getRightDatasetId(), job.getNotes());
                job.setResultId(plan.getId());
            } else if (job.getType() == JobType.RECON_RUN) {
                reconciliationService.execute(job.getResultId());
            }
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            reconJobRepository.save(job);
        } catch (Exception ex) {
            log.error("Job {} failed", jobId, ex);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(truncate(ex.getMessage()));
            job.setCompletedAt(Instant.now());
            reconJobRepository.save(job);
        } finally {
            if (previousEmail == null || previousEmail.isBlank()) {
                UserContext.clear();
            } else {
                UserContext.set(previousEmail);
            }
        }
    }

    private ReconJob runNow(String jobId) {
        process(jobId);
        return reconJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
    }

    @Override
    @Transactional
    public void recoverQueuedJobs() {
        List<ReconJob> pending = reconJobRepository.findByStatusIn(List.of(JobStatus.QUEUED, JobStatus.RUNNING));
        for (ReconJob job : pending) {
            if (job.getStatus() == JobStatus.RUNNING) {
                job.setStatus(JobStatus.QUEUED);
                reconJobRepository.save(job);
            }
            offer(job.getId());
        }
        if (!pending.isEmpty()) {
            log.info("Re-queued {} unfinished jobs", pending.size());
        }
    }

    private void offer(String jobId) {
        IQueue<String> queue = hazelcastInstance.getQueue(HazelcastConfig.JOB_QUEUE);
        if (!queue.offer(jobId)) {
            throw new IllegalStateException("Job queue is full");
        }
    }

    private ReconJob newJob(JobType type) {
        ReconJob job = new ReconJob();
        job.setId(IdUtility.jobId());
        job.setType(type);
        job.setStatus(JobStatus.QUEUED);
        job.setOwnerEmail(UserContext.require());
        job.setCreatedAt(Instant.now());
        return job;
    }

    private String truncate(String message) {
        if (message == null) {
            return "Job failed";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
