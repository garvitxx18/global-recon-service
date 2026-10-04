package global.recon.service.service.implementation;

import global.recon.service.model.CollectionCycle;
import global.recon.service.model.CollectionCycleStatus;
import global.recon.service.model.CollectionItem;
import global.recon.service.model.CollectionItemFetchStatus;
import global.recon.service.model.CollectionPair;
import global.recon.service.model.Dataset;
import global.recon.service.model.ReconCollection;
import global.recon.service.model.ReconRun;
import global.recon.service.repository.CollectionCycleRepository;
import global.recon.service.repository.CollectionItemRepository;
import global.recon.service.repository.CollectionPairRepository;
import global.recon.service.repository.CollectionRepository;
import global.recon.service.service.CollectionExecutionService;
import global.recon.service.service.JobQueueService;
import global.recon.service.service.ReconciliationService;
import global.recon.service.service.ResourceNotFoundException;
import global.recon.service.service.SourceService;
import global.recon.service.utils.IdUtility;
import global.recon.service.utils.JsonCodec;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CollectionExecutionServiceImpl implements CollectionExecutionService {

    private final CollectionRepository collectionRepository;
    private final CollectionCycleRepository collectionCycleRepository;
    private final CollectionPairRepository collectionPairRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final SourceService sourceService;
    private final ReconciliationService reconciliationService;
    private final JobQueueService jobQueueService;
    private final JsonCodec jsonCodec;
    private final TransactionTemplate transactionTemplate;

    public CollectionExecutionServiceImpl(
            CollectionRepository collectionRepository,
            CollectionCycleRepository collectionCycleRepository,
            CollectionPairRepository collectionPairRepository,
            CollectionItemRepository collectionItemRepository,
            SourceService sourceService,
            ReconciliationService reconciliationService,
            @Lazy JobQueueService jobQueueService,
            JsonCodec jsonCodec,
            PlatformTransactionManager transactionManager) {
        this.collectionRepository = collectionRepository;
        this.collectionCycleRepository = collectionCycleRepository;
        this.collectionPairRepository = collectionPairRepository;
        this.collectionItemRepository = collectionItemRepository;
        this.sourceService = sourceService;
        this.reconciliationService = reconciliationService;
        this.jobQueueService = jobQueueService;
        this.jsonCodec = jsonCodec;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional
    public void spawnItems(String cycleId) {
        CollectionCycle cycle = collectionCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + cycleId));
        ReconCollection collection = collectionRepository.findById(cycle.getCollectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        cycle.setStatus(CollectionCycleStatus.RUNNING);
        cycle.setStartedAt(Instant.now());
        List<CollectionPair> pairs = collectionPairRepository.findByCollectionIdOrderBySortOrderAsc(collection.getId());
        cycle.setItemCount(pairs.size());
        collectionCycleRepository.save(cycle);
        String owner = collection.getOwnerEmail();
        for (CollectionPair pair : pairs) {
            CollectionItem item = collectionItemRepository.findByCycleIdAndPairId(cycle.getId(), pair.getId())
                    .orElseGet(() -> {
                        CollectionItem created = new CollectionItem();
                        created.setId(IdUtility.collectionItemId());
                        created.setCycleId(cycle.getId());
                        created.setPairId(pair.getId());
                        created.setLeftValue(pair.getLeftValue());
                        created.setRightValue(pair.getRightValue());
                        created.setFetchStatus(CollectionItemFetchStatus.PENDING);
                        return collectionItemRepository.save(created);
                    });
            if (item.getFetchStatus() == CollectionItemFetchStatus.OK && item.getReconRunId() != null) {
                continue;
            }
            item.setFetchStatus(CollectionItemFetchStatus.PENDING);
            item.setErrorMessage(null);
            collectionItemRepository.save(item);
            jobQueueService.enqueueCollectionItem(item.getId(), cycle.getId(), owner);
        }
        refreshCycle(cycle.getId());
    }

    @Override
    public void runItem(String itemId) {
        CollectionItem snapshot = collectionItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection item not found: " + itemId));
        if (snapshot.getFetchStatus() == CollectionItemFetchStatus.OK && snapshot.getReconRunId() != null) {
            refreshCycle(snapshot.getCycleId());
            return;
        }
        CollectionCycle cycle = collectionCycleRepository.findById(snapshot.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found"));
        ReconCollection collection = collectionRepository.findById(cycle.getCollectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        String asOf = cycle.getAsOfDate().toString();
        try {
            Map<String, String> leftParams = buildParams(
                    collection.getConstantParamsJson(),
                    collection.getLeftIdentityParam(),
                    snapshot.getLeftValue(),
                    collection.getLeftDateParam(),
                    asOf);
            Map<String, String> rightParams = buildParams(
                    collection.getConstantParamsJson(),
                    collection.getRightIdentityParam(),
                    snapshot.getRightValue(),
                    collection.getRightDateParam(),
                    asOf);
            Dataset left = sourceService.ingest(
                    collection.getLeftSourceId(),
                    leftParams,
                    collection.getLeftSourceId() + " " + snapshot.getLeftValue() + " " + asOf,
                    null);
            Dataset right = sourceService.ingest(
                    collection.getRightSourceId(),
                    rightParams,
                    collection.getRightSourceId() + " " + snapshot.getRightValue() + " " + asOf,
                    null);
            ReconRun run = reconciliationService.submit(
                    collection.getPlanId(), left.getId(), right.getId());
            run = reconciliationService.execute(run.getId());
            ReconRun completed = run;
            transactionTemplate.executeWithoutResult(status -> {
                CollectionItem item = collectionItemRepository.findById(itemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Collection item not found: " + itemId));
                item.setLeftDatasetId(left.getId());
                item.setRightDatasetId(right.getId());
                item.setFetchStatus(CollectionItemFetchStatus.OK);
                item.setErrorMessage(null);
                item.setReconRunId(completed.getId());
                item.setReconStatus(completed.getStatus());
                item.setMatchedCount(completed.getMatchedCount());
                item.setBreakCount(completed.getBreakCount());
                item.setOnlyInLeftCount(completed.getOnlyInLeftCount());
                item.setOnlyInRightCount(completed.getOnlyInRightCount());
                collectionItemRepository.save(item);
            });
        } catch (Exception ex) {
            transactionTemplate.executeWithoutResult(status -> {
                CollectionItem item = collectionItemRepository.findById(itemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Collection item not found: " + itemId));
                item.setFetchStatus(CollectionItemFetchStatus.FAILED);
                item.setErrorMessage(truncate(ex.getMessage()));
                collectionItemRepository.save(item);
            });
        } finally {
            refreshCycle(cycle.getId());
        }
    }

    private Map<String, String> buildParams(
            String constantJson,
            String identityParam,
            String identityValue,
            String dateParam,
            String asOf) {
        Map<String, String> params = new LinkedHashMap<>();
        Map<String, Object> constants = jsonCodec.readMap(constantJson);
        for (Map.Entry<String, Object> entry : constants.entrySet()) {
            if (entry.getValue() != null) {
                params.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        params.put(identityParam, identityValue);
        params.put(dateParam, asOf);
        return params;
    }

    private void refreshCycle(String cycleId) {
        transactionTemplate.executeWithoutResult(status -> {
            CollectionCycle cycle = collectionCycleRepository.findById(cycleId).orElse(null);
            if (cycle == null) {
                return;
            }
            List<CollectionItem> items = collectionItemRepository.findByCycleIdOrderByLeftValueAsc(cycleId);
            long failed = items.stream().filter(item -> item.getFetchStatus() == CollectionItemFetchStatus.FAILED).count();
            long pending = items.stream().filter(item -> item.getFetchStatus() == CollectionItemFetchStatus.PENDING).count();
            long breaks = items.stream().filter(item -> item.getBreakCount() > 0).count();
            cycle.setFailedFetchCount(failed);
            cycle.setBreakFundCount(breaks);
            cycle.setItemCount(items.size());
            if (!items.isEmpty() && pending == 0) {
                cycle.setCompletedAt(Instant.now());
                if (failed == 0) {
                    cycle.setStatus(CollectionCycleStatus.COMPLETED);
                } else if (failed == items.size()) {
                    cycle.setStatus(CollectionCycleStatus.FAILED);
                } else {
                    cycle.setStatus(CollectionCycleStatus.PARTIAL);
                }
            }
            collectionCycleRepository.save(cycle);
        });
    }

    private String truncate(String message) {
        if (message == null) {
            return "Collection item failed";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
