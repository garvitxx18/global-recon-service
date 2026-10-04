package global.recon.service.controller;

import global.recon.service.model.Dataset;
import global.recon.service.model.SourceIngestRequest;
import global.recon.service.model.SourceView;
import global.recon.service.service.SourceService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sources")
public class SourceController {

    private final SourceService sourceService;

    public SourceController(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @Operation(summary = "List configured APIs the UI can ingest from")
    @GetMapping
    public List<SourceView> list() {
        return sourceService.listEnabledViews();
    }

    @GetMapping("/{sourceId}")
    public SourceView get(@PathVariable String sourceId) {
        return sourceService.getEnabledView(sourceId);
    }

    @Operation(summary = "Call a configured API and save it as a dataset",
            description = "Single UI endpoint: Feign fetch + JSON ingest. 100–1200 position records ingest in one 10k chunk.")
    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.CREATED)
    public Dataset ingest(@RequestBody SourceIngestRequest request) {
        return sourceService.ingest(request);
    }
}
