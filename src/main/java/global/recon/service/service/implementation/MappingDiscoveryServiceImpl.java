package global.recon.service.service.implementation;

import global.recon.service.config.LlmProperties;
import global.recon.service.feignclient.LlmFeignClient;
import global.recon.service.model.ColumnProfile;
import global.recon.service.model.DataType;
import global.recon.service.model.DatasetProfile;
import global.recon.service.model.DatasetStatus;
import global.recon.service.model.LlmMappingResponse;
import global.recon.service.model.MatchType;
import global.recon.service.model.ReconPlan;
import global.recon.service.service.DatasetService;
import global.recon.service.service.InvalidRequestException;
import global.recon.service.service.LlmDiscoveryException;
import global.recon.service.service.MappingDiscoveryService;
import global.recon.service.service.ProfilingService;
import global.recon.service.service.ReconPlanService;
import global.recon.service.utility.LlmPromptUtility;
import global.recon.service.utility.LlmResponseUtility;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MappingDiscoveryServiceImpl implements MappingDiscoveryService {

    private final DatasetService datasetService;
    private final ProfilingService profilingService;
    private final LlmPromptUtility llmPromptUtility;
    private final LlmResponseUtility llmResponseUtility;
    private final ReconPlanService reconPlanService;
    private final LlmProperties llmProperties;
    private final ObjectProvider<LlmFeignClient> llmFeignClient;

    public MappingDiscoveryServiceImpl(
            DatasetService datasetService,
            ProfilingService profilingService,
            LlmPromptUtility llmPromptUtility,
            LlmResponseUtility llmResponseUtility,
            ReconPlanService reconPlanService,
            LlmProperties llmProperties,
            ObjectProvider<LlmFeignClient> llmFeignClient) {
        this.datasetService = datasetService;
        this.profilingService = profilingService;
        this.llmPromptUtility = llmPromptUtility;
        this.llmResponseUtility = llmResponseUtility;
        this.reconPlanService = reconPlanService;
        this.llmProperties = llmProperties;
        this.llmFeignClient = llmFeignClient;
    }

    @Override
    public ReconPlan discover(String leftDatasetId, String rightDatasetId) {
        if (leftDatasetId == null || rightDatasetId == null) {
            throw new InvalidRequestException("leftDatasetId and rightDatasetId are required");
        }
        if (leftDatasetId.equals(rightDatasetId)) {
            throw new InvalidRequestException("Left and right datasets must be different");
        }
        requireProfiled(leftDatasetId);
        requireProfiled(rightDatasetId);
        DatasetProfile left = profilingService.getProfile(leftDatasetId);
        DatasetProfile right = profilingService.getProfile(rightDatasetId);
        try {
            String raw = llmProperties.isMockEnabled()
                    ? mockResponse(left, right)
                    : invokeLlm(left, right);
            LlmMappingResponse parsed = llmResponseUtility.parse(raw);
            LlmResponseUtility.ValidationResult validated = llmResponseUtility.validate(parsed, left, right);
            return reconPlanService.createDraft(leftDatasetId, rightDatasetId, validated.response(), validated.warnings());
        } catch (LlmDiscoveryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LlmDiscoveryException("Mapping discovery failed: " + ex.getMessage(), ex);
        }
    }

    private void requireProfiled(String datasetId) {
        var dataset = datasetService.getDataset(datasetId);
        if (dataset.getStatus() != DatasetStatus.PROFILED) {
            throw new InvalidRequestException("Dataset " + datasetId + " is not profiled yet");
        }
    }

    private String invokeLlm(DatasetProfile left, DatasetProfile right) {
        LlmFeignClient client = llmFeignClient.getIfAvailable();
        if (client == null) {
            throw new LlmDiscoveryException("LLM Feign client is not available");
        }
        String prompt = llmPromptUtility.buildPrompt(left, right);
        try {
            return client.complete(prompt);
        } catch (LlmDiscoveryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LlmDiscoveryException("LLM API call failed: " + ex.getMessage(), ex);
        }
    }

    private String mockResponse(DatasetProfile left, DatasetProfile right) {
        LlmMappingResponse response = new LlmMappingResponse();
        Set<String> usedRight = new HashSet<>();
        List<ScoredPair> pairs = new ArrayList<>();
        for (ColumnProfile leftColumn : left.getColumns()) {
            ColumnProfile best = null;
            double bestScore = 0;
            for (ColumnProfile rightColumn : right.getColumns()) {
                if (usedRight.contains(rightColumn.getColumnName())) {
                    continue;
                }
                double score = similarity(leftColumn, rightColumn);
                if (score > bestScore) {
                    bestScore = score;
                    best = rightColumn;
                }
            }
            if (best != null && bestScore >= 0.45) {
                usedRight.add(best.getColumnName());
                pairs.add(new ScoredPair(leftColumn, best, bestScore));
            }
        }
        pairs.sort((a, b) -> Double.compare(keyScore(b), keyScore(a)));
        boolean keyChosen = false;
        for (ScoredPair pair : pairs) {
            if (!keyChosen && isKeyCandidate(pair)) {
                LlmMappingResponse.LlmKeyMapping key = new LlmMappingResponse.LlmKeyMapping();
                key.setLeftField(pair.left.getColumnName());
                key.setRightField(pair.right.getColumnName());
                key.setConfidence(clamp(pair.score));
                response.getKeyMappings().add(key);
                keyChosen = true;
            } else {
                LlmMappingResponse.LlmFieldMapping field = new LlmMappingResponse.LlmFieldMapping();
                field.setLeftField(pair.left.getColumnName());
                field.setRightField(pair.right.getColumnName());
                field.setMatchType(matchTypeFor(pair.left, pair.right).name());
                field.setConfidence(clamp(pair.score));
                if (MatchType.NUMERIC_TOLERANCE.name().equals(field.getMatchType())) {
                    field.setTolerance(0.01);
                }
                response.getFieldMappings().add(field);
            }
        }
        if (response.getKeyMappings().isEmpty() && !pairs.isEmpty()) {
            ScoredPair pair = pairs.getFirst();
            LlmMappingResponse.LlmKeyMapping key = new LlmMappingResponse.LlmKeyMapping();
            key.setLeftField(pair.left.getColumnName());
            key.setRightField(pair.right.getColumnName());
            key.setConfidence(clamp(pair.score));
            response.getKeyMappings().add(key);
            response.getFieldMappings().removeIf(mapping ->
                    mapping.getLeftField().equals(pair.left.getColumnName())
                            && mapping.getRightField().equals(pair.right.getColumnName()));
        }
        response.setOverallConfidence(pairs.stream().mapToDouble(pair -> pair.score).average().orElse(0.5));
        return toJson(response);
    }

    private String toJson(LlmMappingResponse response) {
        StringBuilder json = new StringBuilder();
        json.append("{\"keyMappings\":[");
        for (int i = 0; i < response.getKeyMappings().size(); i++) {
            var mapping = response.getKeyMappings().get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"leftField\":\"").append(mapping.getLeftField())
                    .append("\",\"rightField\":\"").append(mapping.getRightField())
                    .append("\",\"confidence\":").append(mapping.getConfidence()).append('}');
        }
        json.append("],\"fieldMappings\":[");
        for (int i = 0; i < response.getFieldMappings().size(); i++) {
            var mapping = response.getFieldMappings().get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"leftField\":\"").append(mapping.getLeftField())
                    .append("\",\"rightField\":\"").append(mapping.getRightField())
                    .append("\",\"matchType\":\"").append(mapping.getMatchType())
                    .append("\",\"confidence\":").append(mapping.getConfidence());
            if (mapping.getTolerance() != null) {
                json.append(",\"tolerance\":").append(mapping.getTolerance());
            }
            json.append('}');
        }
        json.append("],\"overallConfidence\":").append(response.getOverallConfidence()).append('}');
        return json.toString();
    }

    private boolean isKeyCandidate(ScoredPair pair) {
        return pair.left.getUniqueRatio() >= 0.9
                && pair.right.getUniqueRatio() >= 0.9
                && pair.left.getType() == DataType.STRING;
    }

    private double keyScore(ScoredPair pair) {
        double unique = (pair.left.getUniqueRatio() + pair.right.getUniqueRatio()) / 2.0;
        boolean idLike = containsId(pair.left.getColumnName()) || containsId(pair.right.getColumnName());
        return unique + (idLike ? 0.3 : 0) + pair.score;
    }

    private boolean containsId(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("id") || lower.contains("key") || lower.contains("isin");
    }

    private MatchType matchTypeFor(ColumnProfile left, ColumnProfile right) {
        if (left.getType() == DataType.DATE || right.getType() == DataType.DATE
                || left.getType() == DataType.DATETIME || right.getType() == DataType.DATETIME) {
            return MatchType.DATE_NORMALIZED;
        }
        if (left.getType() == DataType.DECIMAL || right.getType() == DataType.DECIMAL
                || left.getType() == DataType.INTEGER || right.getType() == DataType.INTEGER) {
            return MatchType.NUMERIC_TOLERANCE;
        }
        return MatchType.EXACT;
    }

    private double similarity(ColumnProfile left, ColumnProfile right) {
        String leftName = normalizeName(left.getColumnName());
        String rightName = normalizeName(right.getColumnName());
        if (leftName.equals(rightName)) {
            return 0.99;
        }
        if (synonym(leftName, rightName)) {
            return 0.95;
        }
        if (leftName.contains(rightName) || rightName.contains(leftName)) {
            return 0.8;
        }
        if (left.getType() == right.getType()) {
            return 0.4;
        }
        return 0.2;
    }

    private boolean synonym(String left, String right) {
        return Set.of(left, right).equals(Set.of("qty", "quantity"))
                || Set.of(left, right).equals(Set.of("isin", "securityid"))
                || Set.of(left, right).equals(Set.of("price", "tradeprice"))
                || Set.of(left, right).equals(Set.of("ccy", "currency"))
                || Set.of(left, right).equals(Set.of("tradedate", "businessdate"))
                || Set.of(left, right).equals(Set.of("tradeid", "transactionid"));
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private record ScoredPair(ColumnProfile left, ColumnProfile right, double score) {
    }
}
