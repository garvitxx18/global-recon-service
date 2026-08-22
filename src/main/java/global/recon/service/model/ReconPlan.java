package global.recon.service.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recon_plan")
public class ReconPlan {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "left_dataset_id", nullable = false, length = 64)
    private String leftDatasetId;

    @Column(name = "right_dataset_id", nullable = false, length = 64)
    private String rightDatasetId;

    @Column(name = "owner_email", nullable = false, length = 320)
    private String ownerEmail;

    @Lob
    @Column(name = "user_notes")
    private String userNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReconPlanStatus status;

    @Column(name = "overall_confidence")
    private Double overallConfidence;

    @Lob
    @Column(name = "warnings")
    private String warnings;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant approvedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<ReconKeyMapping> keyMappings = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<ReconFieldMapping> fieldMappings = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getUserNotes() {
        return userNotes;
    }

    public void setUserNotes(String userNotes) {
        this.userNotes = userNotes;
    }

    public ReconPlanStatus getStatus() {
        return status;
    }

    public void setStatus(ReconPlanStatus status) {
        this.status = status;
    }

    public Double getOverallConfidence() {
        return overallConfidence;
    }

    public void setOverallConfidence(Double overallConfidence) {
        this.overallConfidence = overallConfidence;
    }

    public String getWarnings() {
        return warnings;
    }

    public void setWarnings(String warnings) {
        this.warnings = warnings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public List<ReconKeyMapping> getKeyMappings() {
        return keyMappings;
    }

    public void setKeyMappings(List<ReconKeyMapping> keyMappings) {
        this.keyMappings = keyMappings;
    }

    public List<ReconFieldMapping> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<ReconFieldMapping> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }
}
