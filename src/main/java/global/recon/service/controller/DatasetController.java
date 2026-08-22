package global.recon.service.controller;

import global.recon.service.model.Dataset;
import global.recon.service.model.DatasetProfile;
import global.recon.service.model.DatasetRecordView;
import global.recon.service.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @Operation(summary = "Upload a dataset", description = "CSV, JSON, or XLSX. Use the file picker for the file part.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Dataset upload(
            @Parameter(
                    description = "Dataset file (CSV, JSON, or XLSX)",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Optional display name")
            @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "Free-text ingest notes for the LLM, e.g. ignore header block, compare only tradeList")
            @RequestParam(value = "notes", required = false) String notes,
            @Parameter(description = "JSON path to each record. tradeList selects that array. order or [].order picks the order object from each list item.")
            @RequestParam(value = "recordPath", required = false) String recordPath) {
        return datasetService.uploadDataset(file, name, notes, recordPath);
    }

    @GetMapping
    public List<Dataset> list() {
        return datasetService.getDatasets();
    }

    @GetMapping("/{datasetId}")
    public Dataset get(@PathVariable String datasetId) {
        return datasetService.getDataset(datasetId);
    }

    @GetMapping("/{datasetId}/profile")
    public DatasetProfile profile(@PathVariable String datasetId) {
        return datasetService.getProfile(datasetId);
    }

    @Operation(summary = "Normalized JSON records", description = "CSV/XLSX rows are stored as JSON objects after ingest.")
    @GetMapping("/{datasetId}/records")
    public Page<DatasetRecordView> records(
            @PathVariable String datasetId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return datasetService.getRecords(
                datasetId,
                PageRequest.of(page, size, Sort.by("rowIndex").ascending()));
    }
}
