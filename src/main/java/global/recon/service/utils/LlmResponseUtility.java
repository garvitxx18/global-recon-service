package global.recon.service.utils;

import global.recon.service.model.DatasetProfile;
import global.recon.service.model.LlmMappingResponse;
import global.recon.service.model.MatchType;
import global.recon.service.service.LlmDiscoveryException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LlmResponseUtility {

    private final ObjectMapper objectMapper;

    public LlmResponseUtility(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LlmMappingResponse parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new LlmDiscoveryException("LLM returned an empty response");
        }
        String json = extractJson(rawResponse);
        try {
            LlmMappingResponse parsed = objectMapper.readValue(json, LlmMappingResponse.class);
            if (parsed == null) {
                throw new LlmDiscoveryException("LLM JSON did not map to a recon plan");
            }
            return parsed;
        } catch (LlmDiscoveryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LlmDiscoveryException("Malformed LLM JSON: " + ex.getMessage(), ex);
        }
    }

    public ValidationResult validate(LlmMappingResponse response, DatasetProfile left, DatasetProfile right) {
        Set<String> leftFields = left.getColumns().stream().map(column -> column.getColumnName()).collect(Collectors.toSet());
        Set<String> rightFields = right.getColumns().stream().map(column -> column.getColumnName()).collect(Collectors.toSet());
        List<String> warnings = new ArrayList<>();
        LlmMappingResponse cleaned = new LlmMappingResponse();
        cleaned.setOverallConfidence(sanitizeConfidence(response.getOverallConfidence(), "overallConfidence", warnings));

        if (response.getKeyMappings() != null) {
            int index = 0;
            for (LlmMappingResponse.LlmKeyMapping mapping : response.getKeyMappings()) {
                if (mapping == null) {
                    continue;
                }
                if (!leftFields.contains(mapping.getLeftField())) {
                    warnings.add("Hallucinated left key field: " + mapping.getLeftField());
                    continue;
                }
                if (!rightFields.contains(mapping.getRightField())) {
                    warnings.add("Hallucinated right key field: " + mapping.getRightField());
                    continue;
                }
                mapping.setConfidence(sanitizeConfidence(mapping.getConfidence(), "keyMappings[" + index + "].confidence", warnings));
                cleaned.getKeyMappings().add(mapping);
                index++;
            }
        }

        if (response.getFieldMappings() != null) {
            int index = 0;
            for (LlmMappingResponse.LlmFieldMapping mapping : response.getFieldMappings()) {
                if (mapping == null) {
                    continue;
                }
                if (!leftFields.contains(mapping.getLeftField())) {
                    warnings.add("Hallucinated left comparison field: " + mapping.getLeftField());
                    continue;
                }
                if (!rightFields.contains(mapping.getRightField())) {
                    warnings.add("Hallucinated right comparison field: " + mapping.getRightField());
                    continue;
                }
                MatchType matchType = parseMatchType(mapping.getMatchType());
                if (matchType == null) {
                    warnings.add("Unsupported matchType '" + mapping.getMatchType() + "' for "
                            + mapping.getLeftField() + " <-> " + mapping.getRightField());
                    continue;
                }
                mapping.setMatchType(matchType.name());
                mapping.setConfidence(sanitizeConfidence(mapping.getConfidence(), "fieldMappings[" + index + "].confidence", warnings));
                cleaned.getFieldMappings().add(mapping);
                index++;
            }
        }

        if (cleaned.getKeyMappings().isEmpty()) {
            warnings.add("The LLM did not return a valid reconciliation key. Select a common field from each dataset to join the same records.");
        }
        return new ValidationResult(cleaned, warnings);
    }

    public String extractJson(String rawResponse) {
        String trimmed = rawResponse.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        throw new LlmDiscoveryException("LLM response did not contain JSON");
    }

    private Double sanitizeConfidence(Double confidence, String field, List<String> warnings) {
        if (confidence == null) {
            return null;
        }
        if (confidence < 0 || confidence > 1) {
            warnings.add(field + " was outside 0..1 and was dropped");
            return null;
        }
        return confidence;
    }

    private MatchType parseMatchType(String matchType) {
        if (matchType == null || matchType.isBlank()) {
            return MatchType.EXACT;
        }
        try {
            return MatchType.valueOf(matchType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record ValidationResult(LlmMappingResponse response, List<String> warnings) {
    }
}
