package global.recon.service.controller;

import global.recon.service.model.Dataset;
import global.recon.service.model.DatasetProfile;
import global.recon.service.service.DatasetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dataset upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) {
        return datasetService.uploadDataset(file, name);
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
}
