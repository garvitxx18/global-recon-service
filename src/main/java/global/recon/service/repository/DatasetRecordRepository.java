package global.recon.service.repository;

import global.recon.service.model.DatasetRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRecordRepository extends JpaRepository<DatasetRecord, String> {

    Page<DatasetRecord> findByDatasetIdOrderByRowIndexAsc(String datasetId, Pageable pageable);

    long countByDatasetId(String datasetId);
}
