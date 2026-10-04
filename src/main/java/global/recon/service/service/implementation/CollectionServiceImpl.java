package global.recon.service.service.implementation;

import global.recon.service.config.CollectionAccess;
import global.recon.service.config.ReconProperties;
import global.recon.service.config.UserContext;
import global.recon.service.model.CollectionCycle;
import global.recon.service.model.CollectionCycleDetail;
import global.recon.service.model.CollectionCycleStatus;
import global.recon.service.model.CollectionDatePolicy;
import global.recon.service.model.CollectionDetail;
import global.recon.service.model.CollectionMember;
import global.recon.service.model.CollectionMemberRole;
import global.recon.service.model.CollectionPair;
import global.recon.service.model.CollectionPairRequest;
import global.recon.service.model.CreateCollectionCycleRequest;
import global.recon.service.model.CreateCollectionRequest;
import global.recon.service.model.Dataset;
import global.recon.service.model.DatasetSourceKind;
import global.recon.service.model.ReconCollection;
import global.recon.service.model.ReconJob;
import global.recon.service.model.ReconPlan;
import global.recon.service.model.ReconPlanStatus;
import global.recon.service.model.Source;
import global.recon.service.model.SourceParam;
import global.recon.service.model.UpdateCollectionRequest;
import global.recon.service.repository.CollectionCycleRepository;
import global.recon.service.repository.CollectionItemRepository;
import global.recon.service.repository.CollectionMemberRepository;
import global.recon.service.repository.CollectionPairRepository;
import global.recon.service.repository.CollectionRepository;
import global.recon.service.service.CollectionService;
import global.recon.service.service.DatasetService;
import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.JobQueueService;
import global.recon.service.service.ReconPlanService;
import global.recon.service.service.ResourceNotFoundException;
import global.recon.service.service.SourceService;
import global.recon.service.utils.IdUtility;
import global.recon.service.utils.JsonCodec;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionMemberRepository collectionMemberRepository;
    private final CollectionPairRepository collectionPairRepository;
    private final CollectionCycleRepository collectionCycleRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final CollectionAccess collectionAccess;
    private final ReconPlanService reconPlanService;
    private final DatasetService datasetService;
    private final SourceService sourceService;
    private final JobQueueService jobQueueService;
    private final JsonCodec jsonCodec;
    private final ReconProperties reconProperties;

    public CollectionServiceImpl(
            CollectionRepository collectionRepository,
            CollectionMemberRepository collectionMemberRepository,
            CollectionPairRepository collectionPairRepository,
            CollectionCycleRepository collectionCycleRepository,
            CollectionItemRepository collectionItemRepository,
            CollectionAccess collectionAccess,
            ReconPlanService reconPlanService,
            DatasetService datasetService,
            SourceService sourceService,
            @Lazy JobQueueService jobQueueService,
            JsonCodec jsonCodec,
            ReconProperties reconProperties) {
        this.collectionRepository = collectionRepository;
        this.collectionMemberRepository = collectionMemberRepository;
        this.collectionPairRepository = collectionPairRepository;
        this.collectionCycleRepository = collectionCycleRepository;
        this.collectionItemRepository = collectionItemRepository;
        this.collectionAccess = collectionAccess;
        this.reconPlanService = reconPlanService;
        this.datasetService = datasetService;
        this.sourceService = sourceService;
        this.jobQueueService = jobQueueService;
        this.jsonCodec = jsonCodec;
        this.reconProperties = reconProperties;
    }

    @Override
    @Transactional
    public ReconCollection create(CreateCollectionRequest request) {
        if (request == null || !hasText(request.getName()) || !hasText(request.getPlanId())) {
            throw new InvalidRequestException("name and planId are required");
        }
        if (request.getPairs() == null || request.getPairs().isEmpty()) {
            throw new InvalidRequestException("At least one identity pair is required");
        }
        ReconPlan plan = reconPlanService.getPlan(request.getPlanId());
        if (plan.getStatus() != ReconPlanStatus.APPROVED) {
            throw new InvalidRequestException("Only an approved plan can become a collection");
        }
        Dataset left = datasetService.getDataset(plan.getLeftDatasetId());
        Dataset right = datasetService.getDataset(plan.getRightDatasetId());
        if (left.getSourceKind() != DatasetSourceKind.SOURCE || right.getSourceKind() != DatasetSourceKind.SOURCE) {
            throw new InvalidRequestException("Both datasets must come from configured APIs to create a collection");
        }
        sourceService.getEnabled(left.getSourceId());
        sourceService.getEnabled(right.getSourceId());
        Source leftSource = sourceService.getEnabled(left.getSourceId());
        Source rightSource = sourceService.getEnabled(right.getSourceId());
        Instant now = Instant.now();
        ReconCollection collection = new ReconCollection();
        collection.setId(IdUtility.collectionId());
        collection.setName(request.getName().trim());
        collection.setOwnerEmail(UserContext.require());
        collection.setPlanId(plan.getId());
        collection.setLeftSourceId(leftSource.getId());
        collection.setRightSourceId(rightSource.getId());
        collection.setLeftIdentityParam(requireKnownParam(leftSource, request.getLeftIdentityParam(), "leftIdentityParam"));
        collection.setRightIdentityParam(requireKnownParam(rightSource, request.getRightIdentityParam(), "rightIdentityParam"));
        collection.setLeftDateParam(requireKnownParam(leftSource, request.getLeftDateParam(), "leftDateParam"));
        collection.setRightDateParam(requireKnownParam(rightSource, request.getRightDateParam(), "rightDateParam"));
        collection.setDatePolicy(request.getDatePolicy() == null ? CollectionDatePolicy.T1 : request.getDatePolicy());
        collection.setScheduleCron(blankToNull(request.getScheduleCron()));
        collection.setConstantParamsJson(jsonCodec.write(request.getConstantParams()));
        collection.setCreatedAt(now);
        collection.setUpdatedAt(now);
        collectionRepository.save(collection);

        CollectionMember owner = new CollectionMember();
        owner.setId(IdUtility.collectionMemberId());
        owner.setCollectionId(collection.getId());
        owner.setEmail(collection.getOwnerEmail());
        owner.setRole(CollectionMemberRole.OWNER);
        collectionMemberRepository.save(owner);
        if (request.getMemberEmails() != null) {
            for (String raw : request.getMemberEmails()) {
                if (!hasText(raw)) {
                    continue;
                }
                String email = raw.trim().toLowerCase(Locale.ROOT);
                if (email.equals(collection.getOwnerEmail())) {
                    continue;
                }
                CollectionMember member = new CollectionMember();
                member.setId(IdUtility.collectionMemberId());
                member.setCollectionId(collection.getId());
                member.setEmail(email);
                member.setRole(CollectionMemberRole.VIEWER);
                collectionMemberRepository.save(member);
            }
        }
        savePairs(collection.getId(), request.getPairs());
        return collection;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReconCollection> list() {
        return collectionRepository.findVisibleTo(UserContext.require());
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionDetail get(String collectionId) {
        ReconCollection collection = load(collectionId);
        collectionAccess.assertCanView(collection);
        CollectionDetail detail = new CollectionDetail();
        detail.setCollection(collection);
        detail.setMembers(collectionMemberRepository.findByCollectionIdOrderByEmailAsc(collectionId));
        detail.setPairs(collectionPairRepository.findByCollectionIdOrderBySortOrderAsc(collectionId));
        collectionCycleRepository.findByCollectionIdOrderByAsOfDateDesc(collectionId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .ifPresent(detail::setLastCycle);
        return detail;
    }

    @Override
    @Transactional
    public ReconCollection update(String collectionId, UpdateCollectionRequest request) {
        ReconCollection collection = load(collectionId);
        collectionAccess.assertCanEdit(collection);
        if (request == null) {
            throw new InvalidRequestException("Update body is required");
        }
        if (hasText(request.getName())) {
            collection.setName(request.getName().trim());
        }
        if (request.getDatePolicy() != null) {
            collection.setDatePolicy(request.getDatePolicy());
        }
        if (request.getScheduleCron() != null) {
            collection.setScheduleCron(blankToNull(request.getScheduleCron()));
        }
        if (request.getMemberEmails() != null) {
            collectionMemberRepository.deleteByCollectionIdAndRole(collectionId, CollectionMemberRole.VIEWER);
            Set<String> emails = new LinkedHashSet<>();
            for (String raw : request.getMemberEmails()) {
                if (!hasText(raw)) {
                    continue;
                }
                String email = raw.trim().toLowerCase(Locale.ROOT);
                if (email.equals(collection.getOwnerEmail()) || !emails.add(email)) {
                    continue;
                }
                CollectionMember member = new CollectionMember();
                member.setId(IdUtility.collectionMemberId());
                member.setCollectionId(collectionId);
                member.setEmail(email);
                member.setRole(CollectionMemberRole.VIEWER);
                collectionMemberRepository.save(member);
            }
        }
        collection.setUpdatedAt(Instant.now());
        return collectionRepository.save(collection);
    }

    @Override
    @Transactional
    public ReconCollection replacePairs(String collectionId, List<CollectionPairRequest> pairs) {
        ReconCollection collection = load(collectionId);
        collectionAccess.assertCanEdit(collection);
        if (pairs == null || pairs.isEmpty()) {
            throw new InvalidRequestException("At least one identity pair is required");
        }
        collectionPairRepository.deleteByCollectionId(collectionId);
        savePairs(collectionId, pairs);
        collection.setUpdatedAt(Instant.now());
        return collectionRepository.save(collection);
    }

    @Override
    @Transactional
    public ReconJob startCycle(String collectionId, CreateCollectionCycleRequest request) {
        ReconCollection collection = load(collectionId);
        collectionAccess.assertCanEdit(collection);
        LocalDate asOf = resolveAsOf(collection, request);
        CollectionCycle cycle = collectionCycleRepository
                .findByCollectionIdAndAsOfDate(collectionId, asOf)
                .orElseGet(() -> {
                    CollectionCycle created = new CollectionCycle();
                    created.setId(IdUtility.collectionCycleId());
                    created.setCollectionId(collectionId);
                    created.setAsOfDate(asOf);
                    created.setStatus(CollectionCycleStatus.QUEUED);
                    created.setItemCount(0);
                    return collectionCycleRepository.save(created);
                });
        if (cycle.getStatus() == CollectionCycleStatus.RUNNING) {
            throw new InvalidRequestException("A cycle for " + asOf + " is already running");
        }
        cycle.setStatus(CollectionCycleStatus.QUEUED);
        cycle.setStartedAt(Instant.now());
        cycle.setCompletedAt(null);
        collectionCycleRepository.save(cycle);
        ReconJob job = jobQueueService.enqueueCollectionCycle(cycle.getId(), collection.getOwnerEmail());
        cycle.setJobId(job.getId());
        collectionCycleRepository.save(cycle);
        return job;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CollectionCycle> listCycles(String collectionId, Pageable pageable) {
        ReconCollection collection = load(collectionId);
        collectionAccess.assertCanView(collection);
        return collectionCycleRepository.findByCollectionIdOrderByAsOfDateDesc(collectionId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionCycleDetail getCycle(String collectionId, String cycleId) {
        ReconCollection collection = load(collectionId);
        collectionAccess.assertCanView(collection);
        CollectionCycle cycle = collectionCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + cycleId));
        if (!collectionId.equals(cycle.getCollectionId())) {
            throw new ResourceNotFoundException("Cycle not found: " + cycleId);
        }
        CollectionCycleDetail detail = new CollectionCycleDetail();
        detail.setCycle(cycle);
        detail.setItems(collectionItemRepository.findByCycleIdOrderByLeftValueAsc(cycleId));
        return detail;
    }

    private void savePairs(String collectionId, List<CollectionPairRequest> pairs) {
        int order = 0;
        for (CollectionPairRequest row : pairs) {
            if (row == null || !hasText(row.getLeftValue()) || !hasText(row.getRightValue())) {
                throw new InvalidRequestException("Each pair needs leftValue and rightValue");
            }
            CollectionPair pair = new CollectionPair();
            pair.setId(IdUtility.collectionPairId());
            pair.setCollectionId(collectionId);
            pair.setLeftValue(row.getLeftValue().trim());
            pair.setRightValue(row.getRightValue().trim());
            pair.setSortOrder(order++);
            collectionPairRepository.save(pair);
        }
    }

    private LocalDate resolveAsOf(ReconCollection collection, CreateCollectionCycleRequest request) {
        CollectionDatePolicy policy = collection.getDatePolicy();
        if (policy == CollectionDatePolicy.CALENDAR) {
            if (request == null || request.getAsOfDate() == null) {
                throw new InvalidRequestException("asOfDate is required for CALENDAR collections");
            }
            return request.getAsOfDate();
        }
        if (request != null && request.getAsOfDate() != null) {
            return request.getAsOfDate();
        }
        LocalDate today = LocalDate.now(ZoneId.of(reconProperties.getRetention().getZone()));
        if (policy == CollectionDatePolicy.T2) {
            return today.minusDays(2);
        }
        return today.minusDays(1);
    }

    private ReconCollection load(String collectionId) {
        return collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found: " + collectionId));
    }

    private String requireKnownParam(Source source, String value, String field) {
        String name = requireParam(value, field);
        List<SourceParam> schema = jsonCodec.readList(source.getParamSchemaJson(), SourceParam.class);
        if (schema.isEmpty()) {
            return name;
        }
        boolean known = schema.stream().anyMatch(param -> name.equals(param.getName()));
        if (!known) {
            throw new InvalidRequestException(field + " is not a parameter of source " + source.getId());
        }
        return name;
    }

    private String requireParam(String value, String field) {
        if (!hasText(value)) {
            throw new InvalidRequestException(field + " is required");
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
