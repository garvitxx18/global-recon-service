package global.recon.service.service.implementation;

import global.recon.service.cache.HazelCastCachingService;
import global.recon.service.config.HazelcastConfig;
import global.recon.service.config.ReconProperties;
import global.recon.service.repository.CollectionItemRepository;
import global.recon.service.repository.DatasetColumnRepository;
import global.recon.service.repository.DatasetRecordRepository;
import global.recon.service.repository.DatasetRepository;
import global.recon.service.repository.ReconPlanRepository;
import global.recon.service.repository.ReconResultRepository;
import global.recon.service.repository.ReconRunRepository;
import global.recon.service.service.DatasetRetentionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DatasetRetentionServiceImpl implements DatasetRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DatasetRetentionServiceImpl.class);

    private final ReconProperties reconProperties;
    private final DatasetRepository datasetRepository;
    private final DatasetRecordRepository datasetRecordRepository;
    private final DatasetColumnRepository datasetColumnRepository;
    private final ReconPlanRepository reconPlanRepository;
    private final ReconRunRepository reconRunRepository;
    private final ReconResultRepository reconResultRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final HazelCastCachingService<Object> cachingService;
    private final TransactionTemplate transactionTemplate;

    public DatasetRetentionServiceImpl(
            ReconProperties reconProperties,
            DatasetRepository datasetRepository,
            DatasetRecordRepository datasetRecordRepository,
            DatasetColumnRepository datasetColumnRepository,
            ReconPlanRepository reconPlanRepository,
            ReconRunRepository reconRunRepository,
            ReconResultRepository reconResultRepository,
            CollectionItemRepository collectionItemRepository,
            HazelCastCachingService<Object> cachingService,
            PlatformTransactionManager transactionManager) {
        this.reconProperties = reconProperties;
        this.datasetRepository = datasetRepository;
        this.datasetRecordRepository = datasetRecordRepository;
        this.datasetColumnRepository = datasetColumnRepository;
        this.reconPlanRepository = reconPlanRepository;
        this.reconRunRepository = reconRunRepository;
        this.reconResultRepository = reconResultRepository;
        this.collectionItemRepository = collectionItemRepository;
        this.cachingService = cachingService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public int purgeExpired() {
        ReconProperties.Retention retention = reconProperties.getRetention();
        Instant cutoff = Instant.now().minus(Math.max(1, retention.getKeepDays()), ChronoUnit.DAYS);
        Set<String> keep = protectedDatasetIds();
        int batchSize = Math.max(1, retention.getBatchSize());
        int total = 0;
        while (true) {
            Integer batch = transactionTemplate.execute(status -> purgeBatch(cutoff, keep, batchSize));
            int count = batch == null ? 0 : batch;
            if (count == 0) {
                break;
            }
            total += count;
        }
        log.info("Dataset retention complete cutoff={} keptPlanDatasets={} purgedDatasets={}",
                cutoff, keep.size(), total);
        return total;
    }

    int purgeBatch(Instant cutoff, Set<String> keep, int batchSize) {
        List<String> ids = keep.isEmpty()
                ? datasetRepository.findIdsCreatedBefore(cutoff, PageRequest.of(0, batchSize))
                : datasetRepository.findIdsCreatedBeforeExcluding(cutoff, keep, PageRequest.of(0, batchSize));
        if (ids.isEmpty()) {
            return 0;
        }
        List<String> runIds = reconRunRepository.findIdsByDatasetIds(ids);
        if (!runIds.isEmpty()) {
            reconResultRepository.deleteByRunIdIn(runIds);
        }
        collectionItemRepository.clearDatasetIds(ids);
        reconRunRepository.clearDatasetIds(ids);
        for (String datasetId : ids) {
            datasetRecordRepository.deleteByDatasetId(datasetId);
            datasetColumnRepository.deleteByDatasetId(datasetId);
            cachingService.evict(HazelcastConfig.DATASET_PROFILE_MAP, datasetId);
        }
        datasetRepository.deleteAllById(ids);
        log.info("Dataset retention batch size={} runResultsCleared={}", ids.size(), runIds.size());
        return ids.size();
    }

    private Set<String> protectedDatasetIds() {
        Set<String> keep = new HashSet<>();
        keep.addAll(reconPlanRepository.findAllLeftDatasetIds());
        keep.addAll(reconPlanRepository.findAllRightDatasetIds());
        keep.remove(null);
        return keep;
    }
}
