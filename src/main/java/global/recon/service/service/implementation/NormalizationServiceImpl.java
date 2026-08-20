package global.recon.service.service.implementation;

import global.recon.service.config.ReconProperties;
import global.recon.service.model.Dataset;
import global.recon.service.model.DatasetRecord;
import global.recon.service.repository.DatasetRecordRepository;
import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.NormalizationService;
import global.recon.service.utility.CsvUtility;
import global.recon.service.utility.IdUtility;
import global.recon.service.utility.JsonCodec;
import global.recon.service.utility.JsonUtility;
import global.recon.service.utility.ProfileUtility;
import global.recon.service.utility.RowCallback;
import global.recon.service.utility.XlsxUtility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NormalizationServiceImpl implements NormalizationService {

    private final CsvUtility csvUtility;
    private final JsonUtility jsonUtility;
    private final XlsxUtility xlsxUtility;
    private final JsonCodec jsonCodec;
    private final ProfileUtility profileUtility;
    private final DatasetRecordRepository datasetRecordRepository;
    private final ReconProperties reconProperties;

    @PersistenceContext
    private EntityManager entityManager;

    public NormalizationServiceImpl(
            CsvUtility csvUtility,
            JsonUtility jsonUtility,
            XlsxUtility xlsxUtility,
            JsonCodec jsonCodec,
            ProfileUtility profileUtility,
            DatasetRecordRepository datasetRecordRepository,
            ReconProperties reconProperties) {
        this.csvUtility = csvUtility;
        this.jsonUtility = jsonUtility;
        this.xlsxUtility = xlsxUtility;
        this.jsonCodec = jsonCodec;
        this.profileUtility = profileUtility;
        this.datasetRecordRepository = datasetRecordRepository;
        this.reconProperties = reconProperties;
    }

    @Override
    @Transactional
    public long normalize(Dataset dataset, InputStream inputStream, Map<String, ProfileUtility.ColumnAccumulator> accumulators) {
        AtomicLong rowIndex = new AtomicLong();
        List<DatasetRecord> buffer = new ArrayList<>(reconProperties.getChunkSize());
        RowCallback callback = row -> {
            if (row == null || row.isEmpty()) {
                return;
            }
            DatasetRecord record = new DatasetRecord();
            record.setId(IdUtility.recordId());
            record.setDatasetId(dataset.getId());
            record.setRowIndex(rowIndex.getAndIncrement());
            record.setPayloadJson(jsonCodec.write(row));
            buffer.add(record);
            profileUtility.observe(accumulators, row);
            if (buffer.size() >= reconProperties.getChunkSize()) {
                persistChunk(buffer);
            }
        };
        try {
            switch (dataset.getFormat()) {
                case CSV -> csvUtility.streamRows(inputStream, callback);
                case JSON -> jsonUtility.streamRows(inputStream, callback);
                case XLSX -> xlsxUtility.streamRows(inputStream, callback);
            }
        } catch (InvalidRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidRequestException("Failed to parse " + dataset.getFormat() + " file: " + ex.getMessage());
        }
        persistChunk(buffer);
        if (rowIndex.get() == 0) {
            throw new InvalidRequestException("Dataset contained no records");
        }
        return rowIndex.get();
    }

    private void persistChunk(List<DatasetRecord> buffer) {
        if (buffer.isEmpty()) {
            return;
        }
        datasetRecordRepository.saveAll(buffer);
        entityManager.flush();
        entityManager.clear();
        buffer.clear();
    }
}
