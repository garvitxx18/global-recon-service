package global.recon.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "recon_run", indexes = {
        @Index(name = "idx_recon_run_owner", columnList = "owner_email")
})
public class ReconRun {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "recon_plan_id", nullable = false, length = 64)
    private String reconPlanId;

    @Column(name = "owner_email", nullable = false, length = 320)
    private String ownerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReconRunStatus status;

    @Column(name = "matched_count")
    private long matchedCount;

    @Column(name = "break_count")
    private long breakCount;

    @Column(name = "only_in_left_count")
    private long onlyInLeftCount;

    @Column(name = "only_in_right_count")
    private long onlyInRightCount;

    @Column(name = "duplicate_left_count")
    private long duplicateLeftCount;

    @Column(name = "duplicate_right_count")
    private long duplicateRightCount;

    @Column(name = "ambiguous_count")
    private long ambiguousCount;

    @Column(name = "processed_count")
    private long processedCount;

    @Column(length = 255)
    private String name;

    @Column(name = "saved_at")
    private Instant savedAt;

    @Column(length = 1024)
    private String errorMessage;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReconPlanId() {
        return reconPlanId;
    }

    public void setReconPlanId(String reconPlanId) {
        this.reconPlanId = reconPlanId;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public ReconRunStatus getStatus() {
        return status;
    }

    public void setStatus(ReconRunStatus status) {
        this.status = status;
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

    public long getDuplicateLeftCount() {
        return duplicateLeftCount;
    }

    public void setDuplicateLeftCount(long duplicateLeftCount) {
        this.duplicateLeftCount = duplicateLeftCount;
    }

    public long getDuplicateRightCount() {
        return duplicateRightCount;
    }

    public void setDuplicateRightCount(long duplicateRightCount) {
        this.duplicateRightCount = duplicateRightCount;
    }

    public long getAmbiguousCount() {
        return ambiguousCount;
    }

    public void setAmbiguousCount(long ambiguousCount) {
        this.ambiguousCount = ambiguousCount;
    }

    public long getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(long processedCount) {
        this.processedCount = processedCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(Instant savedAt) {
        this.savedAt = savedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
