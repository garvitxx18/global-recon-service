package global.recon.service.service.implementation;

import global.recon.service.cache.HazelCastCachingService;
import global.recon.service.config.HazelcastConfig;
import global.recon.service.config.OwnerAccess;
import global.recon.service.config.UserContext;
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
import global.recon.service.utils.IdUtility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class ReconPlanServiceImpl implements ReconPlanService {

    private final ReconPlanRepository reconPlanRepository;
    private final HazelCastCachingService<Object> cachingService;

    public ReconPlanServiceImpl(
            ReconPlanRepository reconPlanRepository,
            HazelCastCachingService<Object> cachingService) {
        this.reconPlanRepository = reconPlanRepository;
        this.cachingService = cachingService;
    }

    @Override
    @Transactional
    public ReconPlan createDraft(
            String leftDatasetId,
            String rightDatasetId,
            LlmMappingResponse mapping,
            List<String> warnings,
            String userNotes) {
        ReconPlan plan = new ReconPlan();
        plan.setId(IdUtility.planId());
        plan.setLeftDatasetId(leftDatasetId);
        plan.setRightDatasetId(rightDatasetId);
        plan.setOwnerEmail(UserContext.require());
        plan.setUserNotes(hasText(userNotes) ? userNotes.trim() : null);
        plan.setStatus(ReconPlanStatus.DRAFT);
        plan.setOverallConfidence(mapping.getOverallConfidence());
        plan.setWarnings(warnings == null || warnings.isEmpty() ? null : String.join(" | ", warnings));
        plan.setCreatedAt(Instant.now());
        applyMappings(plan, mapping);
        return reconPlanRepository.save(plan);
    }

    @Override
    @Transactional
    public ReconPlan createEmptyDraft(String leftDatasetId, String rightDatasetId) {
        if (leftDatasetId == null || rightDatasetId == null || leftDatasetId.isBlank() || rightDatasetId.isBlank()) {
            throw new InvalidRequestException("leftDatasetId and rightDatasetId are required");
        }
        if (leftDatasetId.equals(rightDatasetId)) {
            throw new InvalidRequestException("Left and right datasets must be different");
        }
        return createDraft(leftDatasetId, rightDatasetId, new LlmMappingResponse(), List.of(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public ReconPlan getPlan(String planId) {
        ReconPlan plan = reconPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Recon plan not found: " + planId));
        OwnerAccess.assertOwns(plan.getOwnerEmail());
        return plan;
    }

    @Override
    @Transactional
    public ReconPlan updatePlan(String planId, UpdateReconPlanRequest request) {
        ReconPlan plan = reconPlanRepository.findById(planId).orElse(null);
        if (plan == null) {
            if (!hasText(request.getLeftDatasetId()) || !hasText(request.getRightDatasetId())) {
                throw new InvalidRequestException("leftDatasetId and rightDatasetId are required to create a plan");
            }
            if (request.getLeftDatasetId().equals(request.getRightDatasetId())) {
                throw new InvalidRequestException("Left and right datasets must be different");
            }
            plan = new ReconPlan();
            plan.setId(hasText(planId) ? planId : IdUtility.planId());
            plan.setLeftDatasetId(request.getLeftDatasetId().trim());
            plan.setRightDatasetId(request.getRightDatasetId().trim());
            plan.setCreatedAt(Instant.now());
            plan.setStatus(ReconPlanStatus.DRAFT);
            plan.setOwnerEmail(UserContext.require());
        } else {
            OwnerAccess.assertOwns(plan.getOwnerEmail());
            if (plan.getStatus() == ReconPlanStatus.APPROVED) {
                plan.setStatus(ReconPlanStatus.DRAFT);
                plan.setApprovedAt(null);
            }
        }
        if (request.getUserNotes() != null) {
            plan.setUserNotes(hasText(request.getUserNotes()) ? request.getUserNotes().trim() : null);
        }
        List<ReconKeyMappingRequest> keys = request.getKeyMappings() == null
                ? List.of()
                : request.getKeyMappings().stream().filter(this::completeKey).toList();
        if (keys.isEmpty()) {
            throw new InvalidRequestException("Set at least one match key before saving the plan");
        }
        plan.getKeyMappings().clear();
        plan.getFieldMappings().clear();
        int keyOrder = 0;
        for (ReconKeyMappingRequest mappingRequest : keys) {
            ReconKeyMapping mapping = new ReconKeyMapping();
            mapping.setId(IdUtility.keyMappingId());
            mapping.setPlan(plan);
            mapping.setLeftField(mappingRequest.getLeftField().trim());
            mapping.setRightField(mappingRequest.getRightField().trim());
            mapping.setConfidence(mappingRequest.getConfidence());
            mapping.setSortOrder(keyOrder++);
            plan.getKeyMappings().add(mapping);
        }
        int fieldOrder = 0;
        if (request.getFieldMappings() != null) {
            for (ReconFieldMappingRequest mappingRequest : request.getFieldMappings()) {
                if (!completeField(mappingRequest)) {
                    continue;
                }
                ReconFieldMapping mapping = new ReconFieldMapping();
                mapping.setId(IdUtility.fieldMappingId());
                mapping.setPlan(plan);
                mapping.setLeftField(mappingRequest.getLeftField().trim());
                mapping.setRightField(mappingRequest.getRightField().trim());
                mapping.setMatchType(mappingRequest.getMatchType() == null ? MatchType.EXACT : mappingRequest.getMatchType());
                mapping.setTolerance(mappingRequest.getTolerance());
                if (mapping.getMatchType() == MatchType.NUMERIC_TOLERANCE && mapping.getTolerance() == null) {
                    mapping.setTolerance(BigDecimal.ZERO);
                }
                mapping.setConfidence(mappingRequest.getConfidence());
                mapping.setIncluded(mappingRequest.getIncluded() == null || mappingRequest.getIncluded());
                mapping.setSortOrder(fieldOrder++);
                plan.getFieldMappings().add(mapping);
            }
        }
        plan.setStatus(ReconPlanStatus.DRAFT);
        ReconPlan saved = reconPlanRepository.save(plan);
        if (Boolean.TRUE.equals(request.getApprove())) {
            return approve(saved.getId());
        }
        return saved;
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
        cachingService.put(HazelcastConfig.RECON_PLAN_MAP, saved.getId(), saved.getId());
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
            entity.setMatchType(resolveMatchType(fieldMapping.getMatchType()));
            if (fieldMapping.getTolerance() != null) {
                entity.setTolerance(BigDecimal.valueOf(fieldMapping.getTolerance()));
            } else if (entity.getMatchType() == MatchType.NUMERIC_TOLERANCE) {
                entity.setTolerance(BigDecimal.ZERO);
            }
            entity.setConfidence(fieldMapping.getConfidence());
            entity.setIncluded(true);
            entity.setSortOrder(fieldOrder++);
            plan.getFieldMappings().add(entity);
        }
    }

    private MatchType resolveMatchType(String matchType) {
        if (matchType == null || matchType.isBlank()) {
            return MatchType.EXACT;
        }
        try {
            return MatchType.valueOf(matchType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return MatchType.EXACT;
        }
    }

    private boolean completeKey(ReconKeyMappingRequest mapping) {
        return mapping != null && hasText(mapping.getLeftField()) && hasText(mapping.getRightField());
    }

    private boolean completeField(ReconFieldMappingRequest mapping) {
        return mapping != null && hasText(mapping.getLeftField()) && hasText(mapping.getRightField());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
