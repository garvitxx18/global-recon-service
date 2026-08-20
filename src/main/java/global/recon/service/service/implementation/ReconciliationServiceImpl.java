package global.recon.service.service.implementation;

import global.recon.service.config.HazelcastConfig;
import global.recon.service.config.ReconProperties;
import global.recon.service.model.DatasetRecord;
import global.recon.service.model.ReconDifference;
import global.recon.service.model.ReconFieldMapping;
import global.recon.service.model.ReconPlan;
import global.recon.service.model.ReconPlanStatus;
import global.recon.service.model.ReconResult;
import global.recon.service.model.ReconRun;
import global.recon.service.model.ReconRunStatus;
import global.recon.service.model.ReconStatus;
import global.recon.service.repository.DatasetRecordRepository;
import global.recon.service.repository.ReconRunRepository;
import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.ReconPlanService;
import global.recon.service.service.ReconResultService;
import global.recon.service.service.ReconciliationService;
import global.recon.service.service.ResourceNotFoundException;
import global.recon.service.utility.ComparisonUtility;
import global.recon.service.utility.IdUtility;
import global.recon.service.utility.JsonCodec;
import global.recon.service.utility.ReconKeyUtility;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReconciliationServiceImpl implements ReconciliationService {

    private final ReconPlanService reconPlanService;
    private final ReconRunRepository reconRunRepository;
    private final DatasetRecordRepository datasetRecordRepository;
    private final ReconResultService reconResultService;
    private final ReconKeyUtility reconKeyUtility;
    private final ComparisonUtility comparisonUtility;
    private final JsonCodec jsonCodec;
    private final HazelcastInstance hazelcastInstance;
    private final ReconProperties reconProperties;

    public ReconciliationServiceImpl(
            ReconPlanService reconPlanService,
            ReconRunRepository reconRunRepository,
            DatasetRecordRepository datasetRecordRepository,
            ReconResultService reconResultService,
            ReconKeyUtility reconKeyUtility,
            ComparisonUtility comparisonUtility,
            JsonCodec jsonCodec,
            HazelcastInstance hazelcastInstance,
            ReconProperties reconProperties) {
        this.reconPlanService = reconPlanService;
        this.reconRunRepository = reconRunRepository;
        this.datasetRecordRepository = datasetRecordRepository;
        this.reconResultService = reconResultService;
        this.reconKeyUtility = reconKeyUtility;
        this.comparisonUtility = comparisonUtility;
        this.jsonCodec = jsonCodec;
        this.hazelcastInstance = hazelcastInstance;
        this.reconProperties = reconProperties;
    }

    @Override
    @Transactional
    public ReconRun run(String reconPlanId) {
        ReconPlan plan = reconPlanService.getPlan(reconPlanId);
        if (plan.getStatus() != ReconPlanStatus.APPROVED) {
            throw new InvalidRequestException("Only an approved recon plan can start reconciliation");
        }
        ReconRun run = new ReconRun();
        run.setId(IdUtility.runId());
        run.setReconPlanId(plan.getId());
        run.setStatus(ReconRunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        reconRunRepository.save(run);

        String indexMapName = "recon-index-" + run.getId();
        IMap<String, List<String>> index = hazelcastInstance.getMap(indexMapName);
        IMap<String, Long> progress = hazelcastInstance.getMap(HazelcastConfig.RECON_PROGRESS_MAP);
        try {
            indexRightSide(plan, index);
            Counters counters = processLeftSide(plan, run, index, progress);
            processUnusedRight(run, index, counters);
            applyCounters(run, counters);
            run.setStatus(ReconRunStatus.COMPLETED);
            run.setCompletedAt(Instant.now());
            progress.put(run.getId(), counters.processed);
            return reconRunRepository.save(run);
        } catch (Exception ex) {
            run.setStatus(ReconRunStatus.FAILED);
            run.setErrorMessage(ex.getMessage() == null ? "Reconciliation failed" : ex.getMessage());
            run.setCompletedAt(Instant.now());
            reconRunRepository.save(run);
            throw new InvalidRequestException("Reconciliation failed: " + ex.getMessage());
        } finally {
            index.destroy();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReconRun getRun(String runId) {
        return reconRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Recon run not found: " + runId));
    }

    private void indexRightSide(ReconPlan plan, IMap<String, List<String>> index) {
        int page = 0;
        while (true) {
            var batch = datasetRecordRepository.findByDatasetIdOrderByRowIndexAsc(
                    plan.getRightDatasetId(), PageRequest.of(page, reconProperties.getChunkSize()));
            if (batch.isEmpty()) {
                break;
            }
            for (DatasetRecord record : batch) {
                Map<String, Object> payload = jsonCodec.readMap(record.getPayloadJson());
                String key = reconKeyUtility.buildRightKey(payload, plan.getKeyMappings());
                List<String> ids = new ArrayList<>(index.getOrDefault(key, List.of()));
                ids.add(record.getId());
                index.put(key, ids);
            }
            if (!batch.hasNext()) {
                break;
            }
            page++;
        }
    }

    private Counters processLeftSide(
            ReconPlan plan,
            ReconRun run,
            IMap<String, List<String>> index,
            IMap<String, Long> progress) {
        Counters counters = new Counters();
        Set<String> seenLeftKeys = new HashSet<>();
        List<ReconResult> buffer = new ArrayList<>();
        int page = 0;
        while (true) {
            var batch = datasetRecordRepository.findByDatasetIdOrderByRowIndexAsc(
                    plan.getLeftDatasetId(), PageRequest.of(page, reconProperties.getChunkSize()));
            if (batch.isEmpty()) {
                break;
            }
            for (DatasetRecord leftRecord : batch) {
                Map<String, Object> leftPayload = jsonCodec.readMap(leftRecord.getPayloadJson());
                String key = reconKeyUtility.buildLeftKey(leftPayload, plan.getKeyMappings());
                counters.processed++;
                if (!seenLeftKeys.add(key)) {
                    buffer.add(result(run.getId(), ReconStatus.DUPLICATE_LEFT, leftRecord.getId(), null, key, List.of()));
                    counters.duplicateLeft++;
                    flushIfNeeded(buffer);
                    continue;
                }
                List<String> rightIds = index.getOrDefault(key, List.of());
                if (rightIds.isEmpty()) {
                    buffer.add(result(run.getId(), ReconStatus.ONLY_IN_LEFT, leftRecord.getId(), null, key, List.of()));
                    counters.onlyInLeft++;
                } else if (rightIds.size() > 1) {
                    buffer.add(result(run.getId(), ReconStatus.AMBIGUOUS, leftRecord.getId(), null, key, List.of()));
                    counters.ambiguous++;
                } else {
                    String rightId = rightIds.getFirst();
                    DatasetRecord rightRecord = datasetRecordRepository.findById(rightId)
                            .orElseThrow(() -> new ResourceNotFoundException("Right record not found: " + rightId));
                    Map<String, Object> rightPayload = jsonCodec.readMap(rightRecord.getPayloadJson());
                    List<ReconDifference> differences = compare(plan, leftPayload, rightPayload);
                    if (differences.isEmpty()) {
                        buffer.add(result(run.getId(), ReconStatus.MATCHED, leftRecord.getId(), rightId, key, List.of()));
                        counters.matched++;
                    } else {
                        buffer.add(result(run.getId(), ReconStatus.BREAK, leftRecord.getId(), rightId, key, differences));
                        counters.breaks++;
                    }
                    index.put(key, List.of("USED:" + rightId));
                }
                flushIfNeeded(buffer);
            }
            progress.put(run.getId(), counters.processed);
            if (!batch.hasNext()) {
                break;
            }
            page++;
        }
        reconResultService.persistBatch(buffer);
        return counters;
    }

    private void processUnusedRight(
            ReconRun run,
            IMap<String, List<String>> index,
            Counters counters) {
        List<ReconResult> buffer = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : index.entrySet()) {
            List<String> ids = entry.getValue();
            if (ids == null || ids.isEmpty()) {
                continue;
            }
            boolean used = ids.size() == 1 && ids.getFirst().startsWith("USED:");
            if (used) {
                continue;
            }
            boolean duplicate = ids.size() > 1;
            for (String rightId : ids) {
                ReconStatus status = duplicate ? ReconStatus.DUPLICATE_RIGHT : ReconStatus.ONLY_IN_RIGHT;
                buffer.add(result(run.getId(), status, null, rightId, entry.getKey(), List.of()));
                if (duplicate) {
                    counters.duplicateRight++;
                } else {
                    counters.onlyInRight++;
                }
                flushIfNeeded(buffer);
            }
        }
        reconResultService.persistBatch(buffer);
    }

    private List<ReconDifference> compare(ReconPlan plan, Map<String, Object> left, Map<String, Object> right) {
        List<ReconDifference> differences = new ArrayList<>();
        for (ReconFieldMapping mapping : plan.getFieldMappings()) {
            Object leftValue = left.get(mapping.getLeftField());
            Object rightValue = right.get(mapping.getRightField());
            if (!comparisonUtility.matches(leftValue, rightValue, mapping.getMatchType(), mapping.getTolerance())) {
                differences.add(new ReconDifference(
                        mapping.getLeftField(),
                        mapping.getRightField(),
                        leftValue == null ? null : String.valueOf(leftValue),
                        rightValue == null ? null : String.valueOf(rightValue)));
            }
        }
        return differences;
    }

    private ReconResult result(
            String runId,
            ReconStatus status,
            String leftRecordId,
            String rightRecordId,
            String key,
            List<ReconDifference> differences) {
        ReconResult result = new ReconResult();
        result.setId(IdUtility.resultId());
        result.setRunId(runId);
        result.setStatus(status);
        result.setLeftRecordId(leftRecordId);
        result.setRightRecordId(rightRecordId);
        result.setReconKey(key);
        result.setDifferencesJson(differences == null || differences.isEmpty() ? null : jsonCodec.write(differences));
        return result;
    }

    private void flushIfNeeded(List<ReconResult> buffer) {
        if (buffer.size() >= reconProperties.getChunkSize()) {
            reconResultService.persistBatch(new ArrayList<>(buffer));
            buffer.clear();
        }
    }

    private void applyCounters(ReconRun run, Counters counters) {
        run.setMatchedCount(counters.matched);
        run.setBreakCount(counters.breaks);
        run.setOnlyInLeftCount(counters.onlyInLeft);
        run.setOnlyInRightCount(counters.onlyInRight);
        run.setDuplicateLeftCount(counters.duplicateLeft);
        run.setDuplicateRightCount(counters.duplicateRight);
        run.setAmbiguousCount(counters.ambiguous);
        run.setProcessedCount(counters.processed);
    }

    private static final class Counters {
        private long matched;
        private long breaks;
        private long onlyInLeft;
        private long onlyInRight;
        private long duplicateLeft;
        private long duplicateRight;
        private long ambiguous;
        private long processed;
    }
}
