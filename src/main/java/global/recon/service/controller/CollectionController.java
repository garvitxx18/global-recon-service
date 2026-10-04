package global.recon.service.controller;

import global.recon.service.model.CollectionCycle;
import global.recon.service.model.CollectionCycleDetail;
import global.recon.service.model.CollectionDetail;
import global.recon.service.model.CollectionItem;
import global.recon.service.model.CollectionPairRequest;
import global.recon.service.model.CreateCollectionCycleRequest;
import global.recon.service.model.CreateCollectionRequest;
import global.recon.service.model.ReconCollection;
import global.recon.service.model.UpdateCollectionRequest;
import global.recon.service.model.ReconJob;
import global.recon.service.model.ReconResult;
import global.recon.service.model.ReconStatus;
import global.recon.service.service.CollectionService;
import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.ReconResultService;
import global.recon.service.service.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/collections")
public class CollectionController {

    private final CollectionService collectionService;
    private final ReconResultService reconResultService;

    public CollectionController(CollectionService collectionService, ReconResultService reconResultService) {
        this.collectionService = collectionService;
        this.reconResultService = reconResultService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReconCollection create(@RequestBody CreateCollectionRequest request) {
        return collectionService.create(request);
    }

    @GetMapping
    public List<ReconCollection> list() {
        return collectionService.list();
    }

    @GetMapping("/{collectionId}")
    public CollectionDetail get(@PathVariable String collectionId) {
        return collectionService.get(collectionId);
    }

    @PutMapping("/{collectionId}")
    public ReconCollection update(
            @PathVariable String collectionId,
            @RequestBody UpdateCollectionRequest request) {
        return collectionService.update(collectionId, request);
    }

    @PutMapping("/{collectionId}/pairs")
    public ReconCollection replacePairs(
            @PathVariable String collectionId,
            @RequestBody List<CollectionPairRequest> pairs) {
        return collectionService.replacePairs(collectionId, pairs);
    }

    @Operation(summary = "Run a collection cycle now")
    @PostMapping("/{collectionId}/cycles")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ReconJob startCycle(
            @PathVariable String collectionId,
            @RequestBody(required = false) CreateCollectionCycleRequest request) {
        return collectionService.startCycle(collectionId, request);
    }

    @GetMapping("/{collectionId}/cycles")
    public Page<CollectionCycle> listCycles(
            @PathVariable String collectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return collectionService.listCycles(collectionId, PageRequest.of(page, size));
    }

    @GetMapping("/{collectionId}/cycles/{cycleId}")
    public CollectionCycleDetail getCycle(
            @PathVariable String collectionId,
            @PathVariable String cycleId) {
        return collectionService.getCycle(collectionId, cycleId);
    }

    @GetMapping("/{collectionId}/cycles/{cycleId}/items/{itemId}/summary")
    public Map<String, Long> itemSummary(
            @PathVariable String collectionId,
            @PathVariable String cycleId,
            @PathVariable String itemId) {
        CollectionItem item = requireItem(collectionId, cycleId, itemId);
        if (item.getReconRunId() == null) {
            throw new InvalidRequestException("This pair has not produced a recon run yet");
        }
        return reconResultService.summarizeByRunId(item.getReconRunId());
    }

    @GetMapping("/{collectionId}/cycles/{cycleId}/items/{itemId}/results")
    public Page<ReconResult> itemResults(
            @PathVariable String collectionId,
            @PathVariable String cycleId,
            @PathVariable String itemId,
            @RequestParam(value = "status", required = false) ReconStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        CollectionItem item = requireItem(collectionId, cycleId, itemId);
        if (item.getReconRunId() == null) {
            throw new InvalidRequestException("This pair has not produced a recon run yet");
        }
        return reconResultService.getResults(
                item.getReconRunId(),
                status,
                PageRequest.of(page, size, Sort.by("id").ascending()));
    }

    private CollectionItem requireItem(String collectionId, String cycleId, String itemId) {
        CollectionCycleDetail detail = collectionService.getCycle(collectionId, cycleId);
        return detail.getItems().stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Collection item not found: " + itemId));
    }
}
