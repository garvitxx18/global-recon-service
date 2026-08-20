package global.recon.service.service.implementation;

import global.recon.service.config.HazelcastConfig;
import global.recon.service.model.Dataset;
import global.recon.service.model.DatasetFormat;
import global.recon.service.model.DatasetProfile;
import global.recon.service.model.DatasetStatus;
import global.recon.service.repository.DatasetRepository;
import global.recon.service.service.DatasetService;
import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.NormalizationService;
import global.recon.service.service.ProfilingService;
import global.recon.service.service.ResourceNotFoundException;
import global.recon.service.utility.IdUtility;
import global.recon.service.utility.ProfileUtility;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DatasetServiceImpl implements DatasetService {

    private final DatasetRepository datasetRepository;
    private final NormalizationService normalizationService;
    private final ProfilingService profilingService;
    private final HazelcastInstance hazelcastInstance;

    public DatasetServiceImpl(
            DatasetRepository datasetRepository,
            NormalizationService normalizationService,
            ProfilingService profilingService,
            HazelcastInstance hazelcastInstance) {
        this.datasetRepository = datasetRepository;
        this.normalizationService = normalizationService;
        this.profilingService = profilingService;
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    @Transactional
    public Dataset uploadDataset(MultipartFile file, String name) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Dataset file is required");
        }
        String originalFilename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        DatasetFormat format = detectFormat(originalFilename, file.getContentType());
        Instant now = Instant.now();
        Dataset dataset = new Dataset();
        dataset.setId(IdUtility.datasetId());
        dataset.setName(name == null || name.isBlank() ? originalFilename : name.trim());
        dataset.setOriginalFilename(originalFilename);
        dataset.setFormat(format);
        dataset.setStatus(DatasetStatus.UPLOADING);
        dataset.setCreatedAt(now);
        dataset.setUpdatedAt(now);
        datasetRepository.save(dataset);

        try (InputStream inputStream = file.getInputStream()) {
            Map<String, ProfileUtility.ColumnAccumulator> accumulators = new LinkedHashMap<>();
            long rows = normalizationService.normalize(dataset, inputStream, accumulators);
            dataset.setRowCount(rows);
            dataset.setStatus(DatasetStatus.NORMALIZED);
            dataset.setUpdatedAt(Instant.now());
            datasetRepository.save(dataset);

            DatasetProfile profile = profilingService.saveProfile(dataset, accumulators);
            hazelcastInstance.getMap(HazelcastConfig.DATASET_PROFILE_MAP).put(dataset.getId(), profile);
            dataset.setStatus(DatasetStatus.PROFILED);
            dataset.setUpdatedAt(Instant.now());
            return datasetRepository.save(dataset);
        } catch (InvalidRequestException ex) {
            fail(dataset, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            fail(dataset, ex.getMessage());
            throw new InvalidRequestException("Failed to ingest dataset: " + ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Dataset getDataset(String datasetId) {
        return datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found: " + datasetId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dataset> getDatasets() {
        return datasetRepository.findAll();
    }

    @Override
    @Transactional
    public void updateStatus(String datasetId, DatasetStatus status, String errorMessage) {
        Dataset dataset = getDataset(datasetId);
        dataset.setStatus(status);
        dataset.setErrorMessage(errorMessage);
        dataset.setUpdatedAt(Instant.now());
        datasetRepository.save(dataset);
    }

    @Override
    @Transactional(readOnly = true)
    public DatasetProfile getProfile(String datasetId) {
        getDataset(datasetId);
        DatasetProfile cached = (DatasetProfile) hazelcastInstance.getMap(HazelcastConfig.DATASET_PROFILE_MAP).get(datasetId);
        if (cached != null) {
            return cached;
        }
        DatasetProfile profile = profilingService.getProfile(datasetId);
        hazelcastInstance.getMap(HazelcastConfig.DATASET_PROFILE_MAP).put(datasetId, profile);
        return profile;
    }

    private void fail(Dataset dataset, String message) {
        dataset.setStatus(DatasetStatus.FAILED);
        dataset.setErrorMessage(truncate(message));
        dataset.setUpdatedAt(Instant.now());
        datasetRepository.save(dataset);
    }

    private DatasetFormat detectFormat(String filename, String contentType) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv") || "text/csv".equalsIgnoreCase(contentType)) {
            return DatasetFormat.CSV;
        }
        if (lower.endsWith(".json") || "application/json".equalsIgnoreCase(contentType)) {
            return DatasetFormat.JSON;
        }
        if (lower.endsWith(".xlsx")
                || "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equalsIgnoreCase(contentType)) {
            return DatasetFormat.XLSX;
        }
        throw new InvalidRequestException("Unsupported dataset format. Use CSV, JSON, or XLSX");
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
