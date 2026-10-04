package global.recon.service.repository;

import global.recon.service.model.DatasetRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DatasetRecordRepository extends JpaRepository<DatasetRecord, String> {

    Page<DatasetRecord> findByDatasetIdOrderByRowIndexAsc(String datasetId, Pageable pageable);

    long countByDatasetId(String datasetId);

    @Modifying
    @Query("delete from DatasetRecord r where r.datasetId = :datasetId")
    int deleteByDatasetId(@Param("datasetId") String datasetId);
}
