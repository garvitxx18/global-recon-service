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
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "dataset", indexes = {
        @Index(name = "idx_dataset_owner", columnList = "owner_email")
})
public class Dataset {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DatasetFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DatasetStatus status;

    @Column(name = "row_count")
    private long rowCount;

    @Column(length = 1024)
    private String errorMessage;

    @Column(name = "owner_email", nullable = false, length = 320)
    private String ownerEmail;

    @Column(name = "ingestion_notes", length = 4000)
    private String ingestionNotes;

    @Column(name = "record_path", length = 255)
    private String recordPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false, length = 16)
    private DatasetSourceKind sourceKind = DatasetSourceKind.FILE;

    @Column(name = "source_id", length = 64)
    private String sourceId;

    @Lob
    @JsonIgnore
    @Column(name = "source_params_json")
    private String sourceParamsJson;

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

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public DatasetFormat getFormat() {
        return format;
    }

    public void setFormat(DatasetFormat format) {
        this.format = format;
    }

    public DatasetStatus getStatus() {
        return status;
    }

    public void setStatus(DatasetStatus status) {
        this.status = status;
    }

    public long getRowCount() {
        return rowCount;
    }

    public void setRowCount(long rowCount) {
        this.rowCount = rowCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getIngestionNotes() {
        return ingestionNotes;
    }

    public void setIngestionNotes(String ingestionNotes) {
        this.ingestionNotes = ingestionNotes;
    }

    public String getRecordPath() {
        return recordPath;
    }

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    public DatasetSourceKind getSourceKind() {
        return sourceKind;
    }

    public void setSourceKind(DatasetSourceKind sourceKind) {
        this.sourceKind = sourceKind;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceParamsJson() {
        return sourceParamsJson;
    }

    public void setSourceParamsJson(String sourceParamsJson) {
        this.sourceParamsJson = sourceParamsJson;
    }

    @Transient
    @JsonProperty("sourceParams")
    public Map<String, Object> getSourceParams() {
        if (sourceParamsJson == null || sourceParamsJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = new tools.jackson.databind.json.JsonMapper().readValue(sourceParamsJson, Map.class);
            return parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
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
