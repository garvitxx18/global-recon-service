package global.recon.service.service.implementation;

import global.recon.service.model.ColumnProfile;
import global.recon.service.model.Dataset;
import global.recon.service.model.DatasetColumn;
import global.recon.service.model.DatasetProfile;
import global.recon.service.repository.DatasetColumnRepository;
import global.recon.service.service.ProfilingService;
import global.recon.service.utility.DataTypeUtility;
import global.recon.service.utility.IdUtility;
import global.recon.service.utility.JsonCodec;
import global.recon.service.utility.ProfileUtility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProfilingServiceImpl implements ProfilingService {

    private final DatasetColumnRepository datasetColumnRepository;
    private final ProfileUtility profileUtility;
    private final DataTypeUtility dataTypeUtility;
    private final JsonCodec jsonCodec;

    public ProfilingServiceImpl(
            DatasetColumnRepository datasetColumnRepository,
            ProfileUtility profileUtility,
            DataTypeUtility dataTypeUtility,
            JsonCodec jsonCodec) {
        this.datasetColumnRepository = datasetColumnRepository;
        this.profileUtility = profileUtility;
        this.dataTypeUtility = dataTypeUtility;
        this.jsonCodec = jsonCodec;
    }

    @Override
    @Transactional
    public DatasetProfile saveProfile(Dataset dataset, Map<String, ProfileUtility.ColumnAccumulator> accumulators) {
        datasetColumnRepository.deleteByDatasetId(dataset.getId());
        List<ColumnProfile> columns = profileUtility.finish(accumulators, dataTypeUtility);
        List<DatasetColumn> entities = new ArrayList<>();
        int position = 0;
        for (ColumnProfile column : columns) {
            DatasetColumn entity = new DatasetColumn();
            entity.setId(IdUtility.columnId());
            entity.setDatasetId(dataset.getId());
            entity.setColumnName(column.getColumnName());
            entity.setDetectedType(column.getType());
            entity.setNullCount(column.getNullCount());
            entity.setNullPercentage(column.getNullPercentage());
            entity.setDistinctCount(column.getDistinctCount());
            entity.setUniqueRatio(column.getUniqueRatio());
            entity.setSampleValuesJson(jsonCodec.write(column.getSampleValues()));
            entity.setMinimum(column.getMinimum());
            entity.setMaximum(column.getMaximum());
            entity.setCommonPatternsJson(jsonCodec.write(column.getCommonPatterns()));
            entity.setOrdinalPosition(position++);
            entities.add(entity);
        }
        datasetColumnRepository.saveAll(entities);
        DatasetProfile profile = new DatasetProfile();
        profile.setDatasetId(dataset.getId());
        profile.setRowCount(dataset.getRowCount());
        profile.setColumns(columns);
        return profile;
    }

    @Override
    @Transactional(readOnly = true)
    public DatasetProfile getProfile(String datasetId) {
        List<DatasetColumn> columns = datasetColumnRepository.findByDatasetIdOrderByOrdinalPositionAsc(datasetId);
        DatasetProfile profile = new DatasetProfile();
        profile.setDatasetId(datasetId);
        List<ColumnProfile> columnProfiles = new ArrayList<>();
        for (DatasetColumn column : columns) {
            ColumnProfile columnProfile = new ColumnProfile();
            columnProfile.setColumnName(column.getColumnName());
            columnProfile.setType(column.getDetectedType());
            columnProfile.setNullCount(column.getNullCount());
            columnProfile.setNullPercentage(column.getNullPercentage());
            columnProfile.setDistinctCount(column.getDistinctCount());
            columnProfile.setUniqueRatio(column.getUniqueRatio());
            columnProfile.setSampleValues(jsonCodec.readStringList(column.getSampleValuesJson()));
            columnProfile.setMinimum(column.getMinimum());
            columnProfile.setMaximum(column.getMaximum());
            columnProfile.setCommonPatterns(jsonCodec.readStringList(column.getCommonPatternsJson()));
            columnProfiles.add(columnProfile);
        }
        profile.setColumns(columnProfiles);
        return profile;
    }
}
