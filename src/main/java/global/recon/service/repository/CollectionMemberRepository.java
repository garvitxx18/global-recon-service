package global.recon.service.repository;

import global.recon.service.model.CollectionMember;
import global.recon.service.model.CollectionMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionMemberRepository extends JpaRepository<CollectionMember, String> {

    List<CollectionMember> findByCollectionIdOrderByEmailAsc(String collectionId);

    Optional<CollectionMember> findByCollectionIdAndEmail(String collectionId, String email);

    void deleteByCollectionId(String collectionId);

    void deleteByCollectionIdAndRole(String collectionId, CollectionMemberRole role);
}
