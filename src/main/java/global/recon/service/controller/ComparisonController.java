package global.recon.service.controller;

import global.recon.service.model.ComparisonSummary;
import global.recon.service.model.SaveComparisonRequest;
import global.recon.service.service.ComparisonService;
import global.recon.service.service.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comparisons")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @Operation(summary = "Save a completed recon run as a named comparison")
    @PostMapping
    public ComparisonSummary save(@RequestBody SaveComparisonRequest request) {
        if (request == null || request.getRunId() == null || request.getRunId().isBlank()) {
            throw new InvalidRequestException("runId is required");
        }
        return comparisonService.save(request.getRunId(), request.getName());
    }

    @Operation(summary = "List saved or recent comparisons")
    @GetMapping
    public Page<ComparisonSummary> list(
            @RequestParam(value = "saved", required = false) Boolean saved,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return comparisonService.list(saved, PageRequest.of(page, size, Sort.by("startedAt").descending()));
    }
}
