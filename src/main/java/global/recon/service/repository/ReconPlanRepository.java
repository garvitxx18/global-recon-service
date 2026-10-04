package global.recon.service.repository;

import global.recon.service.model.ReconPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReconPlanRepository extends JpaRepository<ReconPlan, String> {

    @Query("select p.leftDatasetId from ReconPlan p")
    List<String> findAllLeftDatasetIds();

    @Query("select p.rightDatasetId from ReconPlan p")
    List<String> findAllRightDatasetIds();
}
