package global.recon.service.repository;

import global.recon.service.model.Dataset;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface DatasetRepository extends JpaRepository<Dataset, String> {

    List<Dataset> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);

    @Query("select d.id from Dataset d where d.createdAt < :cutoff order by d.createdAt")
    List<String> findIdsCreatedBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Query("""
            select d.id from Dataset d
            where d.createdAt < :cutoff
              and d.id not in :keepIds
            order by d.createdAt
            """)
    List<String> findIdsCreatedBeforeExcluding(
            @Param("cutoff") Instant cutoff,
            @Param("keepIds") Collection<String> keepIds,
            Pageable pageable);
}
