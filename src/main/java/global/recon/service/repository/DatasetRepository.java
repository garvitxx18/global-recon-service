package global.recon.service.repository;

import global.recon.service.model.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetRepository extends JpaRepository<Dataset, String> {

    List<Dataset> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);
}
