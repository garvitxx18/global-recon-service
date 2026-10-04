package global.recon.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "collection_cycle", uniqueConstraints = {
        @UniqueConstraint(name = "uq_collection_cycle_date", columnNames = {"collection_id", "as_of_date"})
})
public class CollectionCycle {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "collection_id", nullable = false, length = 64)
    private String collectionId;

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CollectionCycleStatus status;

    @Column(name = "job_id", length = 64)
    private String jobId;

    @Column(name = "item_count")
    private long itemCount;

    @Column(name = "failed_fetch_count")
    private long failedFetchCount;

    @Column(name = "break_fund_count")
    private long breakFundCount;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

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

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }

    public CollectionCycleStatus getStatus() {
        return status;
    }

    public void setStatus(CollectionCycleStatus status) {
        this.status = status;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public long getItemCount() {
        return itemCount;
    }

    public void setItemCount(long itemCount) {
        this.itemCount = itemCount;
    }

    public long getFailedFetchCount() {
        return failedFetchCount;
    }

    public void setFailedFetchCount(long failedFetchCount) {
        this.failedFetchCount = failedFetchCount;
    }

    public long getBreakFundCount() {
        return breakFundCount;
    }

    public void setBreakFundCount(long breakFundCount) {
        this.breakFundCount = breakFundCount;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
