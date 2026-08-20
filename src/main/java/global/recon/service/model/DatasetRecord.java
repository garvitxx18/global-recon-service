package global.recon.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "dataset_record", indexes = {
        @Index(name = "idx_dataset_record_dataset", columnList = "dataset_id")
})
public class DatasetRecord {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "dataset_id", nullable = false, length = 64)
    private String datasetId;

    @Column(name = "row_index", nullable = false)
    private long rowIndex;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public long getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(long rowIndex) {
        this.rowIndex = rowIndex;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
