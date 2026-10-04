package global.recon.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "collection_item", uniqueConstraints = {
        @UniqueConstraint(name = "uq_collection_item_pair", columnNames = {"cycle_id", "pair_id"})
}, indexes = {
        @Index(name = "idx_collection_item_run", columnList = "recon_run_id")
})
public class CollectionItem {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "cycle_id", nullable = false, length = 64)
    private String cycleId;

    @Column(name = "pair_id", nullable = false, length = 64)
    private String pairId;

    @Column(name = "left_value", nullable = false, length = 128)
    private String leftValue;

    @Column(name = "right_value", nullable = false, length = 128)
    private String rightValue;

    @Column(name = "left_dataset_id", length = 64)
    private String leftDatasetId;

    @Column(name = "right_dataset_id", length = 64)
    private String rightDatasetId;

    @Column(name = "recon_run_id", length = 64)
    private String reconRunId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fetch_status", nullable = false, length = 32)
    private CollectionItemFetchStatus fetchStatus = CollectionItemFetchStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "recon_status", length = 32)
    private ReconRunStatus reconStatus;

    @Column(name = "matched_count")
    private long matchedCount;

    @Column(name = "break_count")
    private long breakCount;

    @Column(name = "only_in_left_count")
    private long onlyInLeftCount;

    @Column(name = "only_in_right_count")
    private long onlyInRightCount;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCycleId() {
        return cycleId;
    }

    public void setCycleId(String cycleId) {
        this.cycleId = cycleId;
    }

    public String getPairId() {
        return pairId;
    }

    public void setPairId(String pairId) {
        this.pairId = pairId;
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

    public String getLeftDatasetId() {
        return leftDatasetId;
    }

    public void setLeftDatasetId(String leftDatasetId) {
        this.leftDatasetId = leftDatasetId;
    }

    public String getRightDatasetId() {
        return rightDatasetId;
    }

    public void setRightDatasetId(String rightDatasetId) {
        this.rightDatasetId = rightDatasetId;
    }

    public String getReconRunId() {
        return reconRunId;
    }

    public void setReconRunId(String reconRunId) {
        this.reconRunId = reconRunId;
    }

    public CollectionItemFetchStatus getFetchStatus() {
        return fetchStatus;
    }

    public void setFetchStatus(CollectionItemFetchStatus fetchStatus) {
        this.fetchStatus = fetchStatus;
    }

    public ReconRunStatus getReconStatus() {
        return reconStatus;
    }

    public void setReconStatus(ReconRunStatus reconStatus) {
        this.reconStatus = reconStatus;
    }

    public long getMatchedCount() {
        return matchedCount;
    }

    public void setMatchedCount(long matchedCount) {
        this.matchedCount = matchedCount;
    }

    public long getBreakCount() {
        return breakCount;
    }

    public void setBreakCount(long breakCount) {
        this.breakCount = breakCount;
    }

    public long getOnlyInLeftCount() {
        return onlyInLeftCount;
    }

    public void setOnlyInLeftCount(long onlyInLeftCount) {
        this.onlyInLeftCount = onlyInLeftCount;
    }

    public long getOnlyInRightCount() {
        return onlyInRightCount;
    }

    public void setOnlyInRightCount(long onlyInRightCount) {
        this.onlyInRightCount = onlyInRightCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
