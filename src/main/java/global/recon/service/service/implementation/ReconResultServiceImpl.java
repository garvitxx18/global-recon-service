package global.recon.service.service.implementation;

import global.recon.service.model.DatasetRecord;
import global.recon.service.model.ReconResult;
import global.recon.service.model.ReconRun;
import global.recon.service.model.ReconStatus;
import global.recon.service.repository.DatasetRecordRepository;
import global.recon.service.repository.ReconResultRepository;
import global.recon.service.service.ReconResultService;
import global.recon.service.utils.BreakReportCsv;
import global.recon.service.utils.JsonCodec;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReconResultServiceImpl implements ReconResultService {

    private final ReconResultRepository reconResultRepository;
    private final DatasetRecordRepository datasetRecordRepository;
    private final JsonCodec jsonCodec;

    @PersistenceContext
    private EntityManager entityManager;

    public ReconResultServiceImpl(
            ReconResultRepository reconResultRepository,
            DatasetRecordRepository datasetRecordRepository,
            JsonCodec jsonCodec) {
        this.reconResultRepository = reconResultRepository;
        this.datasetRecordRepository = datasetRecordRepository;
        this.jsonCodec = jsonCodec;
    }

    @Override
    @Transactional
    public void persistBatch(List<ReconResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        reconResultRepository.saveAll(results);
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReconResult> getResults(String runId, ReconStatus status, Pageable pageable) {
        Page<ReconResult> page = status == null
                ? reconResultRepository.findByRunId(runId, pageable)
                : reconResultRepository.findByRunIdAndStatus(runId, status, pageable);
        hydratePayloads(page.getContent());
        return page;
    }

    private void hydratePayloads(List<ReconResult> results) {
        Set<String> ids = new HashSet<>();
        for (ReconResult result : results) {
            if (result.getLeftRecordId() != null) {
                ids.add(result.getLeftRecordId());
            }
            if (result.getRightRecordId() != null) {
                ids.add(result.getRightRecordId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<String, DatasetRecord> byId = new HashMap<>();
        for (DatasetRecord record : datasetRecordRepository.findAllById(ids)) {
            byId.put(record.getId(), record);
        }
        for (ReconResult result : results) {
            DatasetRecord left = byId.get(result.getLeftRecordId());
            DatasetRecord right = byId.get(result.getRightRecordId());
            result.setLeftPayload(left == null ? new LinkedHashMap<>() : jsonCodec.readMap(left.getPayloadJson()));
            result.setRightPayload(right == null ? new LinkedHashMap<>() : jsonCodec.readMap(right.getPayloadJson()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> summarize(ReconRun run) {
        Map<String, Long> summary = new LinkedHashMap<>();
        for (ReconStatus status : ReconStatus.values()) {
            summary.put(status.name(), reconResultRepository.countByRunIdAndStatus(run.getId(), status));
        }
        summary.put("PROCESSED", run.getProcessedCount());
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public void writeCsvExport(String runId, ReconStatus status, OutputStream output) throws IOException {
        int page = 0;
        int size = 500;
        try (CSVPrinter printer = BreakReportCsv.open(output)) {
            while (true) {
                Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
                Page<ReconResult> chunk = status == null
                        ? reconResultRepository.findByRunId(runId, pageable)
                        : reconResultRepository.findByRunIdAndStatus(runId, status, pageable);
                for (ReconResult result : chunk.getContent()) {
                    BreakReportCsv.writeResult(printer, result);
                }
                if (!chunk.hasNext()) {
                    break;
                }
                page++;
            }
            printer.flush();
        }
    }
}
