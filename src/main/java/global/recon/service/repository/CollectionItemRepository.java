package global.recon.service.repository;

import global.recon.service.model.CollectionItem;
import global.recon.service.model.CollectionItemFetchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CollectionItemRepository extends JpaRepository<CollectionItem, String> {

    List<CollectionItem> findByCycleIdOrderByLeftValueAsc(String cycleId);

    Optional<CollectionItem> findByCycleIdAndPairId(String cycleId, String pairId);

    Optional<CollectionItem> findByReconRunId(String reconRunId);

    long countByCycleId(String cycleId);

    long countByCycleIdAndFetchStatus(String cycleId, CollectionItemFetchStatus fetchStatus);

    long countByCycleIdAndBreakCountGreaterThan(String cycleId, long breakCount);

    @Modifying
    @Query("""
            update CollectionItem i
               set i.leftDatasetId = case when i.leftDatasetId in :datasetIds then null else i.leftDatasetId end,
                   i.rightDatasetId = case when i.rightDatasetId in :datasetIds then null else i.rightDatasetId end
             where i.leftDatasetId in :datasetIds or i.rightDatasetId in :datasetIds
            """)
    int clearDatasetIds(@Param("datasetIds") Collection<String> datasetIds);
}
