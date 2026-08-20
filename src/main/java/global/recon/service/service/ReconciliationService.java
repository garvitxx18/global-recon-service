package global.recon.service.service;

import global.recon.service.model.ReconRun;

public interface ReconciliationService {

    ReconRun run(String reconPlanId);

    ReconRun getRun(String runId);
}
