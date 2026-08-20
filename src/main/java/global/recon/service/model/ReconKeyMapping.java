package global.recon.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recon_key_mapping")
public class ReconKeyMapping {

    @Id
    @Column(length = 64)
    private String id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ReconPlan plan;

    @Column(name = "left_field", nullable = false)
    private String leftField;

    @Column(name = "right_field", nullable = false)
    private String rightField;

    private Double confidence;

    @Column(name = "sort_order")
    private int sortOrder;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ReconPlan getPlan() {
        return plan;
    }

    public void setPlan(ReconPlan plan) {
        this.plan = plan;
    }

    public String getLeftField() {
        return leftField;
    }

    public void setLeftField(String leftField) {
        this.leftField = leftField;
    }

    public String getRightField() {
        return rightField;
    }

    public void setRightField(String rightField) {
        this.rightField = rightField;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
