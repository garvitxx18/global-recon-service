package global.recon.service.service;

import global.recon.service.model.Dataset;
import global.recon.service.utils.ProfileUtility;

import java.io.InputStream;
import java.util.Map;

public interface NormalizationService {

    long normalize(Dataset dataset, InputStream inputStream, Map<String, ProfileUtility.ColumnAccumulator> accumulators);
}
