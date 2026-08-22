package global.recon.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Lob
    @Column(name = "differences")
    @JsonIgnore
    private String differencesJson;

    @Transient
    private Map<String, Object> leftPayload = new LinkedHashMap<>();

    @Transient
    private Map<String, Object> rightPayload = new LinkedHashMap<>();

    @Transient
    @JsonProperty("differences")
    public List<ReconDifference> getDifferences() {
        if (differencesJson == null || differencesJson.isBlank()) {
            return List.of();
        }
        return JSON.readValue(differencesJson, new TypeReference<List<ReconDifference>>() {
        });
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

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

    @JsonIgnore
    public String getDifferencesJson() {
        return differencesJson;
    }

    public void setDifferencesJson(String differencesJson) {
        this.differencesJson = differencesJson;
    }

    public Map<String, Object> getLeftPayload() {
        return leftPayload;
    }

    public void setLeftPayload(Map<String, Object> leftPayload) {
        this.leftPayload = leftPayload == null ? new LinkedHashMap<>() : leftPayload;
    }

    public Map<String, Object> getRightPayload() {
        return rightPayload;
    }

    public void setRightPayload(Map<String, Object> rightPayload) {
        this.rightPayload = rightPayload == null ? new LinkedHashMap<>() : rightPayload;
    }
}
