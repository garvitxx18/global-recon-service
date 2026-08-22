package global.recon.service.service;

import global.recon.service.model.ReconRun;

public interface ReconciliationService {

    ReconRun submit(String reconPlanId);

    ReconRun execute(String runId);

    ReconRun getRun(String runId);
}
