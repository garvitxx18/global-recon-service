package global.recon.service.repository;

import global.recon.service.model.ReconRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconRunRepository extends JpaRepository<ReconRun, String> {
}
