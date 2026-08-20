package global.recon.service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dataset_column")
public class DatasetColumn {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "dataset_id", nullable = false, length = 64)
    private String datasetId;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Enumerated(EnumType.STRING)
    @Column(name = "detected_type", nullable = false, length = 16)
    private DataType detectedType;

    @Column(name = "null_count")
    private long nullCount;

    @Column(name = "null_percentage")
    private double nullPercentage;

    @Column(name = "distinct_count")
    private long distinctCount;

    @Column(name = "unique_ratio")
    private double uniqueRatio;

    @Column(name = "sample_values", columnDefinition = "TEXT")
    private String sampleValuesJson;

    @Column(name = "min_value")
    private String minimum;

    @Column(name = "max_value")
    private String maximum;

    @Column(name = "common_patterns", columnDefinition = "TEXT")
    private String commonPatternsJson;

    @Column(name = "ordinal_position")
    private int ordinalPosition;

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

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public DataType getDetectedType() {
        return detectedType;
    }

    public void setDetectedType(DataType detectedType) {
        this.detectedType = detectedType;
    }

    public long getNullCount() {
        return nullCount;
    }

    public void setNullCount(long nullCount) {
        this.nullCount = nullCount;
    }

    public double getNullPercentage() {
        return nullPercentage;
    }

    public void setNullPercentage(double nullPercentage) {
        this.nullPercentage = nullPercentage;
    }

    public long getDistinctCount() {
        return distinctCount;
    }

    public void setDistinctCount(long distinctCount) {
        this.distinctCount = distinctCount;
    }

    public double getUniqueRatio() {
        return uniqueRatio;
    }

    public void setUniqueRatio(double uniqueRatio) {
        this.uniqueRatio = uniqueRatio;
    }

    public String getSampleValuesJson() {
        return sampleValuesJson;
    }

    public void setSampleValuesJson(String sampleValuesJson) {
        this.sampleValuesJson = sampleValuesJson;
    }

    public String getMinimum() {
        return minimum;
    }

    public void setMinimum(String minimum) {
        this.minimum = minimum;
    }

    public String getMaximum() {
        return maximum;
    }

    public void setMaximum(String maximum) {
        this.maximum = maximum;
    }

    public String getCommonPatternsJson() {
        return commonPatternsJson;
    }

    public void setCommonPatternsJson(String commonPatternsJson) {
        this.commonPatternsJson = commonPatternsJson;
    }

    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    public void setOrdinalPosition(int ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }
}
