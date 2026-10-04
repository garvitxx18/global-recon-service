package global.recon.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "collection", indexes = {
        @Index(name = "idx_collection_owner", columnList = "owner_email")
})
public class ReconCollection {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "owner_email", nullable = false, length = 320)
    private String ownerEmail;

    @Column(name = "plan_id", nullable = false, length = 64)
    private String planId;

    @Column(name = "left_source_id", nullable = false, length = 64)
    private String leftSourceId;

    @Column(name = "right_source_id", nullable = false, length = 64)
    private String rightSourceId;

    @Column(name = "left_identity_param", nullable = false, length = 64)
    private String leftIdentityParam;

    @Column(name = "right_identity_param", nullable = false, length = 64)
    private String rightIdentityParam;

    @Column(name = "left_date_param", nullable = false, length = 64)
    private String leftDateParam;

    @Column(name = "right_date_param", nullable = false, length = 64)
    private String rightDateParam;

    @Lob
    @JsonIgnore
    @Column(name = "constant_params_json")
    private String constantParamsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_policy", nullable = false, length = 16)
    private CollectionDatePolicy datePolicy;

    @Column(name = "schedule_cron", length = 64)
    private String scheduleCron;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getLeftSourceId() {
        return leftSourceId;
    }

    public void setLeftSourceId(String leftSourceId) {
        this.leftSourceId = leftSourceId;
    }

    public String getRightSourceId() {
        return rightSourceId;
    }

    public void setRightSourceId(String rightSourceId) {
        this.rightSourceId = rightSourceId;
    }

    public String getLeftIdentityParam() {
        return leftIdentityParam;
    }

    public void setLeftIdentityParam(String leftIdentityParam) {
        this.leftIdentityParam = leftIdentityParam;
    }

    public String getRightIdentityParam() {
        return rightIdentityParam;
    }

    public void setRightIdentityParam(String rightIdentityParam) {
        this.rightIdentityParam = rightIdentityParam;
    }

    public String getLeftDateParam() {
        return leftDateParam;
    }

    public void setLeftDateParam(String leftDateParam) {
        this.leftDateParam = leftDateParam;
    }

    public String getRightDateParam() {
        return rightDateParam;
    }

    public void setRightDateParam(String rightDateParam) {
        this.rightDateParam = rightDateParam;
    }

    public String getConstantParamsJson() {
        return constantParamsJson;
    }

    public void setConstantParamsJson(String constantParamsJson) {
        this.constantParamsJson = constantParamsJson;
    }

    @Transient
    @JsonProperty("constantParams")
    public Map<String, Object> getConstantParams() {
        if (constantParamsJson == null || constantParamsJson.isBlank()) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = new tools.jackson.databind.json.JsonMapper().readValue(constantParamsJson, Map.class);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    public CollectionDatePolicy getDatePolicy() {
        return datePolicy;
    }

    public void setDatePolicy(CollectionDatePolicy datePolicy) {
        this.datePolicy = datePolicy;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
