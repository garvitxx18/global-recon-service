package global.recon.service.service;

import global.recon.service.model.Dataset;
import global.recon.service.model.Source;
import global.recon.service.model.SourceIngestRequest;
import global.recon.service.model.SourceView;

import java.util.List;
import java.util.Map;

public interface SourceService {

    List<Source> listEnabled();

    List<SourceView> listEnabledViews();

    Source getEnabled(String sourceId);

    SourceView getEnabledView(String sourceId);

    Dataset ingest(SourceIngestRequest request);

    Dataset ingest(String sourceId, Map<String, String> params, String name, String recordPath);

    Map<String, String> validateParams(Source source, Map<String, String> params);
}
