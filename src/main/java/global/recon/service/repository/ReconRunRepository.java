package global.recon.service.repository;

import global.recon.service.model.ReconRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconRunRepository extends JpaRepository<ReconRun, String> {

    Page<ReconRun> findByOwnerEmail(String ownerEmail, Pageable pageable);

    Page<ReconRun> findByOwnerEmailAndSavedAtIsNotNull(String ownerEmail, Pageable pageable);

    Page<ReconRun> findByOwnerEmailAndSavedAtIsNull(String ownerEmail, Pageable pageable);
}
