package global.recon.service.service;

import global.recon.service.model.ComparisonSummary;
import global.recon.service.model.ReconRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ComparisonService {

    ComparisonSummary save(String runId, String name);

    Page<ComparisonSummary> list(Boolean saved, Pageable pageable);
}
