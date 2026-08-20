package global.recon.service.repository;

import global.recon.service.model.DatasetColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetColumnRepository extends JpaRepository<DatasetColumn, String> {

    List<DatasetColumn> findByDatasetIdOrderByOrdinalPositionAsc(String datasetId);

    void deleteByDatasetId(String datasetId);
}
