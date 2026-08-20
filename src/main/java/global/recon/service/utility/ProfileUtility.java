package global.recon.service.utility;

import global.recon.service.model.ColumnProfile;
import global.recon.service.model.DataType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ProfileUtility {

    private static final int SAMPLE_LIMIT = 8;
    private static final int DISTINCT_CAP = 50_000;
    private static final Pattern ISIN = Pattern.compile("^[A-Z]{2}[A-Z0-9]{9}[0-9]$");
    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");
    private static final Pattern ALPHANUM = Pattern.compile("^[A-Za-z0-9_-]+$");

    public Map<String, ColumnAccumulator> newAccumulators(Set<String> columns) {
        Map<String, ColumnAccumulator> accumulators = new LinkedHashMap<>();
        for (String column : columns) {
            accumulators.put(column, new ColumnAccumulator(column));
        }
        return accumulators;
    }

    public void observe(Map<String, ColumnAccumulator> accumulators, Map<String, Object> row) {
        for (String column : row.keySet()) {
            accumulators.computeIfAbsent(column, ColumnAccumulator::new);
        }
        for (ColumnAccumulator accumulator : accumulators.values()) {
            accumulator.observe(row.get(accumulator.columnName));
        }
    }

    public List<ColumnProfile> finish(Map<String, ColumnAccumulator> accumulators, DataTypeUtility dataTypeUtility) {
        List<ColumnProfile> profiles = new ArrayList<>();
        int position = 0;
        for (ColumnAccumulator accumulator : accumulators.values()) {
            profiles.add(accumulator.toProfile(dataTypeUtility, position++));
        }
        return profiles;
    }

    public static final class ColumnAccumulator {
        private final String columnName;
        private long total;
        private long nullCount;
        private final Set<String> distinct = new LinkedHashSet<>();
        private boolean distinctCapped;
        private final List<String> samples = new ArrayList<>();
        private String minimum;
        private String maximum;
        private final Set<String> patterns = new LinkedHashSet<>();

        public ColumnAccumulator(String columnName) {
            this.columnName = columnName;
        }

        public void observe(Object value) {
            total++;
            if (value == null || String.valueOf(value).isBlank()) {
                nullCount++;
                return;
            }
            String text = String.valueOf(value).trim();
            if (!distinctCapped) {
                distinct.add(text);
                if (distinct.size() >= DISTINCT_CAP) {
                    distinctCapped = true;
                }
            }
            if (samples.size() < SAMPLE_LIMIT) {
                samples.add(text);
            }
            if (minimum == null || text.compareTo(minimum) < 0) {
                minimum = text;
            }
            if (maximum == null || text.compareTo(maximum) > 0) {
                maximum = text;
            }
            patterns.add(classify(text));
        }

        public ColumnProfile toProfile(DataTypeUtility dataTypeUtility, int ignoredPosition) {
            ColumnProfile profile = new ColumnProfile();
            profile.setColumnName(columnName);
            profile.setType(dataTypeUtility.detect(new ArrayList<>(samples)));
            profile.setNullCount(nullCount);
            profile.setNullPercentage(total == 0 ? 0 : (nullCount * 100.0d) / total);
            profile.setDistinctCount(distinct.size());
            long nonNull = total - nullCount;
            profile.setUniqueRatio(nonNull == 0 ? 0 : (double) distinct.size() / nonNull);
            profile.setSampleValues(List.copyOf(samples));
            if (profile.getType() == DataType.INTEGER || profile.getType() == DataType.DECIMAL
                    || profile.getType() == DataType.DATE || profile.getType() == DataType.DATETIME) {
                profile.setMinimum(minimum);
                profile.setMaximum(maximum);
            }
            profile.setCommonPatterns(new ArrayList<>(patterns));
            return profile;
        }

        private String classify(String text) {
            String upper = text.toUpperCase(Locale.ROOT);
            if (ISIN.matcher(upper).matches()) {
                return "ISIN";
            }
            if (DIGITS.matcher(text).matches()) {
                return "DIGITS";
            }
            if (ALPHANUM.matcher(text).matches()) {
                return "ALPHANUMERIC";
            }
            return "FREE_TEXT";
        }
    }
}
