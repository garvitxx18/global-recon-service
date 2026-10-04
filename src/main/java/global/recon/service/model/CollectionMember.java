package global.recon.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "collection_member", uniqueConstraints = {
        @UniqueConstraint(name = "uq_collection_member", columnNames = {"collection_id", "email"})
})
public class CollectionMember {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "collection_id", nullable = false, length = 64)
    private String collectionId;

    @Column(nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CollectionMemberRole role;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CollectionMemberRole getRole() {
        return role;
    }

    public void setRole(CollectionMemberRole role) {
        this.role = role;
    }
}
