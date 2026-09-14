package global.recon.service.service;

import global.recon.service.model.ReconResult;
import global.recon.service.model.ReconRun;
import global.recon.service.model.ReconStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface ReconResultService {

    void persistBatch(List<ReconResult> results);

    Page<ReconResult> getResults(String runId, ReconStatus status, Pageable pageable);

    Map<String, Long> summarize(ReconRun run);

    void writeCsvExport(String runId, ReconStatus status, OutputStream output) throws IOException;
}
