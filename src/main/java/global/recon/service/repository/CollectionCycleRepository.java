package global.recon.service.repository;

import global.recon.service.model.CollectionCycle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CollectionCycleRepository extends JpaRepository<CollectionCycle, String> {

    Optional<CollectionCycle> findByCollectionIdAndAsOfDate(String collectionId, LocalDate asOfDate);

    Page<CollectionCycle> findByCollectionIdOrderByAsOfDateDesc(String collectionId, Pageable pageable);
}
