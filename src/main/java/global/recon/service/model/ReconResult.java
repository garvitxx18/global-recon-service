package global.recon.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "recon_result", indexes = {
        @Index(name = "idx_recon_result_run", columnList = "run_id"),
        @Index(name = "idx_recon_result_run_status", columnList = "run_id, status")
})
public class ReconResult {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReconStatus status;

    @Column(name = "left_record_id", length = 64)
    private String leftRecordId;

    @Column(name = "right_record_id", length = 64)
    private String rightRecordId;

    @Column(name = "recon_key")
    private String reconKey;

    @Column(name = "differences", columnDefinition = "TEXT")
    private String differencesJson;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public ReconStatus getStatus() {
        return status;
    }

    public void setStatus(ReconStatus status) {
        this.status = status;
    }

    public String getLeftRecordId() {
        return leftRecordId;
    }

    public void setLeftRecordId(String leftRecordId) {
        this.leftRecordId = leftRecordId;
    }

    public String getRightRecordId() {
        return rightRecordId;
    }

    public void setRightRecordId(String rightRecordId) {
        this.rightRecordId = rightRecordId;
    }

    public String getReconKey() {
        return reconKey;
    }

    public void setReconKey(String reconKey) {
        this.reconKey = reconKey;
    }

    public String getDifferencesJson() {
        return differencesJson;
    }

    public void setDifferencesJson(String differencesJson) {
        this.differencesJson = differencesJson;
    }
}
