package global.recon.service.service.implementation;

import global.recon.service.config.UserContext;
import global.recon.service.model.ComparisonSummary;
import global.recon.service.model.Dataset;
import global.recon.service.model.ReconPlan;
import global.recon.service.model.ReconRun;
import global.recon.service.repository.DatasetRepository;
import global.recon.service.repository.ReconRunRepository;
import global.recon.service.service.ComparisonService;
import global.recon.service.service.ReconPlanService;
import global.recon.service.service.ReconciliationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ComparisonServiceImpl implements ComparisonService {

    private final ReconciliationService reconciliationService;
    private final ReconPlanService reconPlanService;
    private final ReconRunRepository reconRunRepository;
    private final DatasetRepository datasetRepository;

    public ComparisonServiceImpl(
            ReconciliationService reconciliationService,
            ReconPlanService reconPlanService,
            ReconRunRepository reconRunRepository,
            DatasetRepository datasetRepository) {
        this.reconciliationService = reconciliationService;
        this.reconPlanService = reconPlanService;
        this.reconRunRepository = reconRunRepository;
        this.datasetRepository = datasetRepository;
    }

    @Override
    @Transactional
    public ComparisonSummary save(String runId, String name) {
        ReconRun run = reconciliationService.getRun(runId);
        ReconPlan plan = reconPlanService.getPlan(run.getReconPlanId());
        String resolved = resolveName(name, plan);
        run.setName(resolved);
        run.setSavedAt(Instant.now());
        return toSummary(reconRunRepository.save(run), plan);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComparisonSummary> list(Boolean saved, Pageable pageable) {
        String email = UserContext.require();
        Page<ReconRun> page;
        if (Boolean.TRUE.equals(saved)) {
            page = reconRunRepository.findByOwnerEmailAndSavedAtIsNotNull(email, pageable);
        } else if (Boolean.FALSE.equals(saved)) {
            page = reconRunRepository.findByOwnerEmailAndSavedAtIsNull(email, pageable);
        } else {
            page = reconRunRepository.findByOwnerEmail(email, pageable);
        }
        return page.map(run -> {
            ReconPlan plan = reconPlanService.getPlan(run.getReconPlanId());
            return toSummary(run, plan);
        });
    }

    private String resolveName(String name, ReconPlan plan) {
        if (name != null && !name.isBlank()) {
            return truncate(name.trim());
        }
        String left = datasetName(plan.getLeftDatasetId());
        String right = datasetName(plan.getRightDatasetId());
        return truncate(left + " vs " + right);
    }

    private ComparisonSummary toSummary(ReconRun run, ReconPlan plan) {
        ComparisonSummary summary = new ComparisonSummary();
        summary.setRunId(run.getId());
        summary.setName(run.getName());
        summary.setStatus(run.getStatus());
        summary.setReconPlanId(run.getReconPlanId());
        summary.setLeftDatasetId(plan.getLeftDatasetId());
        summary.setRightDatasetId(plan.getRightDatasetId());
        summary.setLeftDatasetName(datasetName(plan.getLeftDatasetId()));
        summary.setRightDatasetName(datasetName(plan.getRightDatasetId()));
        summary.setMatchedCount(run.getMatchedCount());
        summary.setBreakCount(run.getBreakCount());
        summary.setOnlyInLeftCount(run.getOnlyInLeftCount());
        summary.setOnlyInRightCount(run.getOnlyInRightCount());
        summary.setStartedAt(run.getStartedAt());
        summary.setCompletedAt(run.getCompletedAt());
        summary.setSavedAt(run.getSavedAt());
        return summary;
    }

    private String datasetName(String datasetId) {
        return datasetRepository.findById(datasetId).map(Dataset::getName).orElse(datasetId);
    }

    private String truncate(String value) {
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
