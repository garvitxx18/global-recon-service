package global.recon.service.repository;

import global.recon.service.model.ReconRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReconRunRepository extends JpaRepository<ReconRun, String> {

    Page<ReconRun> findByOwnerEmail(String ownerEmail, Pageable pageable);

    Page<ReconRun> findByOwnerEmailAndSavedAtIsNotNull(String ownerEmail, Pageable pageable);

    Page<ReconRun> findByOwnerEmailAndSavedAtIsNull(String ownerEmail, Pageable pageable);

    @Query("""
            select r.id from ReconRun r
            where r.leftDatasetId in :datasetIds or r.rightDatasetId in :datasetIds
            """)
    List<String> findIdsByDatasetIds(@Param("datasetIds") Collection<String> datasetIds);

    @Modifying
    @Query("""
            update ReconRun r
               set r.leftDatasetId = case when r.leftDatasetId in :datasetIds then null else r.leftDatasetId end,
                   r.rightDatasetId = case when r.rightDatasetId in :datasetIds then null else r.rightDatasetId end
             where r.leftDatasetId in :datasetIds or r.rightDatasetId in :datasetIds
            """)
    int clearDatasetIds(@Param("datasetIds") Collection<String> datasetIds);
}
