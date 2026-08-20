package global.recon.service.repository;

import global.recon.service.model.ReconResult;
import global.recon.service.model.ReconStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconResultRepository extends JpaRepository<ReconResult, String> {

    Page<ReconResult> findByRunId(String runId, Pageable pageable);

    Page<ReconResult> findByRunIdAndStatus(String runId, ReconStatus status, Pageable pageable);

    long countByRunIdAndStatus(String runId, ReconStatus status);
}
