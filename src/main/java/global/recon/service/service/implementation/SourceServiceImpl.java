package global.recon.service.service.implementation;

import global.recon.service.feignclient.ConfiguredSourceClient;
import global.recon.service.model.Dataset;
import global.recon.service.model.Source;
import global.recon.service.model.SourceIngestRequest;
import global.recon.service.model.SourceAuthType;
import global.recon.service.model.SourceParam;
import global.recon.service.model.SourceView;
import global.recon.service.repository.SourceRepository;
import global.recon.service.service.DatasetService;
import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.ResourceNotFoundException;
import global.recon.service.service.SourceService;
import global.recon.service.utils.JsonCodec;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SourceServiceImpl implements SourceService {

    private final SourceRepository sourceRepository;
    private final ConfiguredSourceClient configuredSourceClient;
    private final DatasetService datasetService;
    private final Environment environment;
    private final JsonCodec jsonCodec;

    public SourceServiceImpl(
            SourceRepository sourceRepository,
            ConfiguredSourceClient configuredSourceClient,
            DatasetService datasetService,
            Environment environment,
            JsonCodec jsonCodec) {
        this.sourceRepository = sourceRepository;
        this.configuredSourceClient = configuredSourceClient;
        this.datasetService = datasetService;
        this.environment = environment;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public List<Source> listEnabled() {
        return sourceRepository.findByEnabledTrueOrderByNameAsc();
    }

    @Override
    public List<SourceView> listEnabledViews() {
        return listEnabled().stream().map(this::toView).toList();
    }

    @Override
    public Source getEnabled(String sourceId) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Source not found: " + sourceId));
        if (!source.isEnabled()) {
            throw new InvalidRequestException("Source is disabled: " + sourceId);
        }
        return source;
    }

    @Override
    public SourceView getEnabledView(String sourceId) {
        return toView(getEnabled(sourceId));
    }

    @Override
    public Dataset ingest(SourceIngestRequest request) {
        if (request == null || request.getSourceId() == null || request.getSourceId().isBlank()) {
            throw new InvalidRequestException("sourceId is required");
        }
        return ingest(request.getSourceId(), request.getParams(), request.getName(), request.getRecordPath());
    }

    @Override
    public Dataset ingest(String sourceId, Map<String, String> params, String name, String recordPath) {
        Source source = getEnabled(sourceId);
        Map<String, String> validated = validateParams(source, params);
        String path = recordPath == null || recordPath.isBlank() ? source.getRecordPath() : recordPath;
        byte[] body = configuredSourceClient.fetch(source, validated, resolveSecret(source));
        return datasetService.ingestFromSource(source.getId(), validated, name, path, body);
    }

    @Override
    public Map<String, String> validateParams(Source source, Map<String, String> params) {
        Map<String, String> incoming = new LinkedHashMap<>();
        if (params != null) {
            params.forEach((key, value) -> {
                if (key != null && !key.isBlank()) {
                    incoming.put(key.trim(), value == null ? "" : value);
                }
            });
        }
        List<SourceParam> schema = jsonCodec.readList(source.getParamSchemaJson(), SourceParam.class);
        if (schema.isEmpty()) {
            return incoming;
        }
        Set<String> allowed = schema.stream()
                .map(SourceParam::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());
        for (String key : incoming.keySet()) {
            if (!allowed.contains(key)) {
                throw new InvalidRequestException("Unknown parameter '" + key + "' for source " + source.getId());
            }
        }
        for (SourceParam param : schema) {
            if (param.getName() == null || param.getName().isBlank()) {
                continue;
            }
            String value = incoming.get(param.getName());
            if (param.isRequired() && (value == null || value.isBlank())) {
                throw new InvalidRequestException("Parameter '" + param.getName() + "' is required");
            }
        }
        return incoming;
    }

    private SourceView toView(Source source) {
        return SourceView.from(source, jsonCodec.readList(source.getParamSchemaJson(), SourceParam.class));
    }

    private String resolveSecret(Source source) {
        if (source.getAuthType() == null || source.getAuthType() == SourceAuthType.NONE) {
            return null;
        }
        if (source.getSecretRef() == null || source.getSecretRef().isBlank()) {
            throw new InvalidRequestException("Source " + source.getId() + " is missing secretRef");
        }
        String value = environment.getProperty(source.getSecretRef());
        if (value == null || value.isBlank()) {
            value = System.getenv(source.getSecretRef());
        }
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Secret is not configured for source " + source.getId());
        }
        return value;
    }
}
