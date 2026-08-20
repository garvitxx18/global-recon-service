package global.recon.service.utility;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CsvUtility {

    public void streamRows(InputStream inputStream, RowCallback callback) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .get()
                     .parse(reader)) {
            List<String> headers = parser.getHeaderNames();
            if (headers == null || headers.isEmpty()) {
                throw new IllegalArgumentException("CSV file has no header row");
            }
            for (CSVRecord record : parser) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String header : headers) {
                    String value = record.isMapped(header) ? record.get(header) : null;
                    row.put(header, blankToNull(value));
                }
                callback.accept(row);
            }
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
