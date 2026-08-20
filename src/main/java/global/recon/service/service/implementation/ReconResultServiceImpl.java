package global.recon.service.service.implementation;

import global.recon.service.model.ReconResult;
import global.recon.service.model.ReconRun;
import global.recon.service.model.ReconStatus;
import global.recon.service.repository.ReconResultRepository;
import global.recon.service.service.ReconResultService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReconResultServiceImpl implements ReconResultService {

    private final ReconResultRepository reconResultRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ReconResultServiceImpl(ReconResultRepository reconResultRepository) {
        this.reconResultRepository = reconResultRepository;
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
        if (status == null) {
            return reconResultRepository.findByRunId(runId, pageable);
        }
        return reconResultRepository.findByRunIdAndStatus(runId, status, pageable);
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
}
