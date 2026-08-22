package global.recon.service.model;

import java.time.Instant;

public class ComparisonSummary {

    private String runId;
    private String name;
    private ReconRunStatus status;
    private String reconPlanId;
    private String leftDatasetId;
    private String rightDatasetId;
    private String leftDatasetName;
    private String rightDatasetName;
    private long matchedCount;
    private long breakCount;
    private long onlyInLeftCount;
    private long onlyInRightCount;
    private Instant startedAt;
    private Instant completedAt;
    private Instant savedAt;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ReconRunStatus getStatus() {
        return status;
    }

    public void setStatus(ReconRunStatus status) {
        this.status = status;
    }

    public String getReconPlanId() {
        return reconPlanId;
    }

    public void setReconPlanId(String reconPlanId) {
        this.reconPlanId = reconPlanId;
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

    public String getLeftDatasetName() {
        return leftDatasetName;
    }

    public void setLeftDatasetName(String leftDatasetName) {
        this.leftDatasetName = leftDatasetName;
    }

    public String getRightDatasetName() {
        return rightDatasetName;
    }

    public void setRightDatasetName(String rightDatasetName) {
        this.rightDatasetName = rightDatasetName;
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

    public Instant getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(Instant savedAt) {
        this.savedAt = savedAt;
    }
}
