package global.recon.service.service;

import global.recon.service.model.Dataset;
import global.recon.service.model.DatasetProfile;
import global.recon.service.utility.ProfileUtility;

import java.util.Map;

public interface ProfilingService {

    DatasetProfile saveProfile(Dataset dataset, Map<String, ProfileUtility.ColumnAccumulator> accumulators);

    DatasetProfile getProfile(String datasetId);
}
