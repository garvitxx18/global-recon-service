package global.recon.service.repository;

import global.recon.service.model.JobStatus;
import global.recon.service.model.ReconJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconJobRepository extends JpaRepository<ReconJob, String> {

    List<ReconJob> findByStatusIn(List<JobStatus> statuses);

    Page<ReconJob> findByOwnerEmail(String ownerEmail, Pageable pageable);

    Page<ReconJob> findByOwnerEmailAndStatus(String ownerEmail, JobStatus status, Pageable pageable);
}
