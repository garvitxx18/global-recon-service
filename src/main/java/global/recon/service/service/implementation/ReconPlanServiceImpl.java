package global.recon.service.service.implementation;

import global.recon.service.config.HazelcastConfig;
import global.recon.service.model.LlmMappingResponse;
import global.recon.service.model.MatchType;
import global.recon.service.model.ReconFieldMapping;
import global.recon.service.model.ReconFieldMappingRequest;
import global.recon.service.model.ReconKeyMapping;
import global.recon.service.model.ReconKeyMappingRequest;
import global.recon.service.model.ReconPlan;
import global.recon.service.model.ReconPlanStatus;
import global.recon.service.model.UpdateReconPlanRequest;
import global.recon.service.repository.ReconPlanRepository;
import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.ReconPlanService;
import global.recon.service.service.ResourceNotFoundException;
import global.recon.service.utility.IdUtility;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class ReconPlanServiceImpl implements ReconPlanService {

    private final ReconPlanRepository reconPlanRepository;
    private final HazelcastInstance hazelcastInstance;

    public ReconPlanServiceImpl(ReconPlanRepository reconPlanRepository, HazelcastInstance hazelcastInstance) {
        this.reconPlanRepository = reconPlanRepository;
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    @Transactional
    public ReconPlan createDraft(String leftDatasetId, String rightDatasetId, LlmMappingResponse mapping, List<String> warnings) {
        ReconPlan plan = new ReconPlan();
        plan.setId(IdUtility.planId());
        plan.setLeftDatasetId(leftDatasetId);
        plan.setRightDatasetId(rightDatasetId);
        plan.setStatus(ReconPlanStatus.DRAFT);
        plan.setOverallConfidence(mapping.getOverallConfidence());
        plan.setWarnings(warnings == null || warnings.isEmpty() ? null : String.join(" | ", warnings));
        plan.setCreatedAt(Instant.now());
        applyMappings(plan, mapping);
        return reconPlanRepository.save(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public ReconPlan getPlan(String planId) {
        return reconPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Recon plan not found: " + planId));
    }

    @Override
    @Transactional
    public ReconPlan updatePlan(String planId, UpdateReconPlanRequest request) {
        ReconPlan plan = getPlan(planId);
        if (plan.getStatus() == ReconPlanStatus.APPROVED) {
            throw new InvalidRequestException("Approved recon plans cannot be modified");
        }
        if (request.getKeyMappings() == null || request.getKeyMappings().isEmpty()) {
            throw new InvalidRequestException("At least one key mapping is required");
        }
        plan.getKeyMappings().clear();
        plan.getFieldMappings().clear();
        int keyOrder = 0;
        for (ReconKeyMappingRequest mappingRequest : request.getKeyMappings()) {
            ReconKeyMapping mapping = new ReconKeyMapping();
            mapping.setId(IdUtility.keyMappingId());
            mapping.setPlan(plan);
            mapping.setLeftField(requireField(mappingRequest.getLeftField(), "leftField"));
            mapping.setRightField(requireField(mappingRequest.getRightField(), "rightField"));
            mapping.setConfidence(mappingRequest.getConfidence());
            mapping.setSortOrder(keyOrder++);
            plan.getKeyMappings().add(mapping);
        }
        int fieldOrder = 0;
        if (request.getFieldMappings() != null) {
            for (ReconFieldMappingRequest mappingRequest : request.getFieldMappings()) {
                ReconFieldMapping mapping = new ReconFieldMapping();
                mapping.setId(IdUtility.fieldMappingId());
                mapping.setPlan(plan);
                mapping.setLeftField(requireField(mappingRequest.getLeftField(), "leftField"));
                mapping.setRightField(requireField(mappingRequest.getRightField(), "rightField"));
                mapping.setMatchType(mappingRequest.getMatchType() == null ? MatchType.EXACT : mappingRequest.getMatchType());
                mapping.setTolerance(mappingRequest.getTolerance());
                mapping.setConfidence(mappingRequest.getConfidence());
                mapping.setSortOrder(fieldOrder++);
                plan.getFieldMappings().add(mapping);
            }
        }
        plan.setStatus(ReconPlanStatus.DRAFT);
        return reconPlanRepository.save(plan);
    }

    @Override
    @Transactional
    public ReconPlan approve(String planId) {
        ReconPlan plan = getPlan(planId);
        if (plan.getKeyMappings() == null || plan.getKeyMappings().isEmpty()) {
            throw new InvalidRequestException("Cannot approve a plan without key mappings");
        }
        plan.setStatus(ReconPlanStatus.APPROVED);
        plan.setApprovedAt(Instant.now());
        ReconPlan saved = reconPlanRepository.save(plan);
        hazelcastInstance.getMap(HazelcastConfig.RECON_PLAN_MAP).put(saved.getId(), saved.getId());
        return saved;
    }

    private void applyMappings(ReconPlan plan, LlmMappingResponse mapping) {
        int keyOrder = 0;
        for (LlmMappingResponse.LlmKeyMapping keyMapping : mapping.getKeyMappings()) {
            ReconKeyMapping entity = new ReconKeyMapping();
            entity.setId(IdUtility.keyMappingId());
            entity.setPlan(plan);
            entity.setLeftField(keyMapping.getLeftField());
            entity.setRightField(keyMapping.getRightField());
            entity.setConfidence(keyMapping.getConfidence());
            entity.setSortOrder(keyOrder++);
            plan.getKeyMappings().add(entity);
        }
        int fieldOrder = 0;
        for (LlmMappingResponse.LlmFieldMapping fieldMapping : mapping.getFieldMappings()) {
            ReconFieldMapping entity = new ReconFieldMapping();
            entity.setId(IdUtility.fieldMappingId());
            entity.setPlan(plan);
            entity.setLeftField(fieldMapping.getLeftField());
            entity.setRightField(fieldMapping.getRightField());
            entity.setMatchType(MatchType.valueOf(fieldMapping.getMatchType().toUpperCase(Locale.ROOT)));
            if (fieldMapping.getTolerance() != null) {
                entity.setTolerance(BigDecimal.valueOf(fieldMapping.getTolerance()));
            }
            entity.setConfidence(fieldMapping.getConfidence());
            entity.setSortOrder(fieldOrder++);
            plan.getFieldMappings().add(entity);
        }
    }

    private String requireField(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(name + " is required");
        }
        return value.trim();
    }
}
