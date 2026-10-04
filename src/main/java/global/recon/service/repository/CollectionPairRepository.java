package global.recon.service.repository;

import global.recon.service.model.CollectionPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionPairRepository extends JpaRepository<CollectionPair, String> {

    List<CollectionPair> findByCollectionIdOrderBySortOrderAsc(String collectionId);

    void deleteByCollectionId(String collectionId);
}
