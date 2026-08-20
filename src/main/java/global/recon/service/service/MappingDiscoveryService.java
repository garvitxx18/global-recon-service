package global.recon.service.service;

import global.recon.service.model.ReconPlan;

public interface MappingDiscoveryService {

    ReconPlan discover(String leftDatasetId, String rightDatasetId);
}
