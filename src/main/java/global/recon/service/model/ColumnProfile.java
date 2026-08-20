package global.recon.service.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ColumnProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private String columnName;
    private DataType type;
    private long nullCount;
    private double nullPercentage;
    private long distinctCount;
    private double uniqueRatio;
    private List<String> sampleValues = new ArrayList<>();
    private String minimum;
    private String maximum;
    private List<String> commonPatterns = new ArrayList<>();

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
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

    public List<String> getSampleValues() {
        return sampleValues;
    }

    public void setSampleValues(List<String> sampleValues) {
        this.sampleValues = sampleValues;
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

    public List<String> getCommonPatterns() {
        return commonPatterns;
    }

    public void setCommonPatterns(List<String> commonPatterns) {
        this.commonPatterns = commonPatterns;
    }
}
