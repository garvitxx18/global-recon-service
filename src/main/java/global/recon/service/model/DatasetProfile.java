package global.recon.service.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DatasetProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private String datasetId;
    private long rowCount;
    private List<ColumnProfile> columns = new ArrayList<>();

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public long getRowCount() {
        return rowCount;
    }

    public void setRowCount(long rowCount) {
        this.rowCount = rowCount;
    }

    public List<ColumnProfile> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnProfile> columns) {
        this.columns = columns;
    }
}
