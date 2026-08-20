package global.recon.service.repository;

import global.recon.service.model.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<Dataset, String> {
}
