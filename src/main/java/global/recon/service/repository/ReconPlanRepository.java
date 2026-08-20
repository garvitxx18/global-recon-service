package global.recon.service.repository;

import global.recon.service.model.ReconPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconPlanRepository extends JpaRepository<ReconPlan, String> {
}
