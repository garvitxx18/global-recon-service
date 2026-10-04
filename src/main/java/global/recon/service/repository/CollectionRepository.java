package global.recon.service.repository;

import global.recon.service.model.ReconCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollectionRepository extends JpaRepository<ReconCollection, String> {

    @Query("""
            select distinct c from ReconCollection c
            where c.ownerEmail = :email
               or exists (
                    select 1 from CollectionMember m
                    where m.collectionId = c.id and m.email = :email
               )
            order by c.updatedAt desc
            """)
    List<ReconCollection> findVisibleTo(@Param("email") String email);
}
