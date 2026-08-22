package global.recon.service.service;

import global.recon.service.model.Dataset;
import global.recon.service.model.DatasetProfile;
import global.recon.service.model.DatasetRecordView;
import global.recon.service.model.DatasetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DatasetService {

    Dataset uploadDataset(MultipartFile file, String name, String ingestionNotes, String recordPath);

    Dataset getDataset(String datasetId);

    List<Dataset> getDatasets();

    void updateStatus(String datasetId, DatasetStatus status, String errorMessage);

    DatasetProfile getProfile(String datasetId);

    Page<DatasetRecordView> getRecords(String datasetId, Pageable pageable);
}
