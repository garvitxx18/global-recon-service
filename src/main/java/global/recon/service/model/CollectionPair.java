package global.recon.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "collection_pair", uniqueConstraints = {
        @UniqueConstraint(name = "uq_collection_pair_left", columnNames = {"collection_id", "left_value"}),
        @UniqueConstraint(name = "uq_collection_pair_right", columnNames = {"collection_id", "right_value"})
})
public class CollectionPair {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "collection_id", nullable = false, length = 64)
    private String collectionId;

    @Column(name = "left_value", nullable = false, length = 128)
    private String leftValue;

    @Column(name = "right_value", nullable = false, length = 128)
    private String rightValue;

    @Column(name = "sort_order")
    private int sortOrder;

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

    public String getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(String leftValue) {
        this.leftValue = leftValue;
    }

    public String getRightValue() {
        return rightValue;
    }

    public void setRightValue(String rightValue) {
        this.rightValue = rightValue;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
