package global.recon.service.service;

import global.recon.service.model.ReconRun;

public interface ReconciliationService {

    ReconRun submit(String reconPlanId);

    ReconRun submit(String reconPlanId, String leftDatasetId, String rightDatasetId);

    ReconRun execute(String runId);

    ReconRun getRun(String runId);
}
