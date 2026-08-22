package global.recon.service.utils;

import global.recon.service.model.ColumnProfile;
import global.recon.service.model.DataType;
import global.recon.service.model.DatasetProfile;
import global.recon.service.model.LlmMappingResponse;
import global.recon.service.model.MatchType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class MappingCompletionUtility {

    public void complete(LlmMappingResponse response, DatasetProfile left, DatasetProfile right) {
        if (response.getKeyMappings() == null) {
            response.setKeyMappings(new ArrayList<>());
        }
        if (response.getFieldMappings() == null) {
            response.setFieldMappings(new ArrayList<>());
        }
        Set<String> keyLeft = new HashSet<>();
        Set<String> keyRight = new HashSet<>();
        for (LlmMappingResponse.LlmKeyMapping key : response.getKeyMappings()) {
            if (key.getLeftField() != null) {
                keyLeft.add(key.getLeftField());
            }
            if (key.getRightField() != null) {
                keyRight.add(key.getRightField());
            }
        }
        response.getFieldMappings().removeIf(mapping ->
                keyLeft.contains(mapping.getLeftField()) || keyRight.contains(mapping.getRightField()));

        Set<String> usedLeft = new HashSet<>(keyLeft);
        Set<String> usedRight = new HashSet<>(keyRight);
        for (LlmMappingResponse.LlmFieldMapping mapping : response.getFieldMappings()) {
            usedLeft.add(mapping.getLeftField());
            usedRight.add(mapping.getRightField());
            applyCompareDefaults(mapping, column(left, mapping.getLeftField()), column(right, mapping.getRightField()));
        }

        List<ColumnProfile> leftoverLeft = leftover(left, usedLeft);
        List<ColumnProfile> leftoverRight = leftover(right, usedRight);
        Set<String> claimedRight = new HashSet<>();
        for (ColumnProfile leftColumn : leftoverLeft) {
            ColumnProfile best = null;
            double bestScore = -1;
            for (ColumnProfile rightColumn : leftoverRight) {
                if (claimedRight.contains(rightColumn.getColumnName())) {
                    continue;
                }
                double score = similarity(leftColumn, rightColumn);
                if (score > bestScore) {
                    bestScore = score;
                    best = rightColumn;
                }
            }
            if (best == null) {
                continue;
            }
            claimedRight.add(best.getColumnName());
            LlmMappingResponse.LlmFieldMapping mapping = new LlmMappingResponse.LlmFieldMapping();
            mapping.setLeftField(leftColumn.getColumnName());
            mapping.setRightField(best.getColumnName());
            mapping.setConfidence(clamp(bestScore));
            applyCompareDefaults(mapping, leftColumn, best);
            response.getFieldMappings().add(mapping);
        }
    }

    public MatchType matchTypeFor(ColumnProfile left, ColumnProfile right) {
        if (isDate(left) || isDate(right)) {
            return MatchType.DATE_NORMALIZED;
        }
        if (isNumeric(left) || isNumeric(right)) {
            return MatchType.NUMERIC_TOLERANCE;
        }
        return MatchType.EXACT;
    }

    public boolean isNumeric(ColumnProfile column) {
        return column != null && (column.getType() == DataType.INTEGER || column.getType() == DataType.DECIMAL);
    }

    private boolean isDate(ColumnProfile column) {
        return column != null && (column.getType() == DataType.DATE || column.getType() == DataType.DATETIME);
    }

    private void applyCompareDefaults(
            LlmMappingResponse.LlmFieldMapping mapping,
            ColumnProfile left,
            ColumnProfile right) {
        MatchType matchType = parseMatchType(mapping.getMatchType());
        if (matchType == null) {
            matchType = matchTypeFor(left, right);
        }
        if (isNumeric(left) || isNumeric(right)) {
            matchType = MatchType.NUMERIC_TOLERANCE;
            if (mapping.getTolerance() == null) {
                mapping.setTolerance(0.0);
            }
        }
        mapping.setMatchType(matchType.name());
    }

    private MatchType parseMatchType(String matchType) {
        if (matchType == null || matchType.isBlank()) {
            return null;
        }
        try {
            return MatchType.valueOf(matchType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<ColumnProfile> leftover(DatasetProfile profile, Set<String> used) {
        List<ColumnProfile> leftover = new ArrayList<>();
        if (profile.getColumns() == null) {
            return leftover;
        }
        for (ColumnProfile column : profile.getColumns()) {
            if (!used.contains(column.getColumnName())) {
                leftover.add(column);
            }
        }
        return leftover;
    }

    private ColumnProfile column(DatasetProfile profile, String name) {
        if (profile.getColumns() == null || name == null) {
            return null;
        }
        for (ColumnProfile column : profile.getColumns()) {
            if (name.equals(column.getColumnName())) {
                return column;
            }
        }
        return null;
    }

    public double similarity(ColumnProfile left, ColumnProfile right) {
        String leftName = normalizeName(left.getColumnName());
        String rightName = normalizeName(right.getColumnName());
        if (leftName.equals(rightName)) {
            return 0.99;
        }
        if (synonym(leftName, rightName)) {
            return 0.95;
        }
        if (!leftName.isEmpty() && !rightName.isEmpty()
                && (leftName.contains(rightName) || rightName.contains(leftName))) {
            return 0.8;
        }
        int overlap = tokenOverlap(left.getColumnName(), right.getColumnName());
        if (overlap > 0) {
            return 0.7;
        }
        if (left.getType() == right.getType()) {
            return 0.35;
        }
        return 0.1;
    }

    private boolean synonym(String left, String right) {
        Set<String> pair = Set.of(left, right);
        return pair.equals(Set.of("qty", "quantity"))
                || pair.equals(Set.of("units", "quantity"))
                || pair.equals(Set.of("isin", "securityid"))
                || pair.equals(Set.of("isin", "securitycode"))
                || pair.equals(Set.of("txnref", "transactionreference"))
                || pair.equals(Set.of("txnref", "transactionid"))
                || pair.equals(Set.of("tradedate", "businessdate"))
                || pair.equals(Set.of("side", "direction"))
                || pair.equals(Set.of("price", "tradeprice"))
                || pair.equals(Set.of("ccy", "currency"))
                || pair.equals(Set.of("tradeid", "transactionid"));
    }

    private int tokenOverlap(String left, String right) {
        Set<String> leftTokens = tokens(left);
        Set<String> rightTokens = tokens(right);
        int overlap = 0;
        for (String token : leftTokens) {
            if (rightTokens.contains(token)) {
                overlap++;
            }
        }
        return overlap;
    }

    private Set<String> tokens(String name) {
        String[] parts = name.replaceAll("([a-z])([A-Z])", "$1 $2").split("[\\s_\\-./]+");
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            if (part.length() > 1) {
                tokens.add(part.toLowerCase(Locale.ROOT));
            }
        }
        return tokens;
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
