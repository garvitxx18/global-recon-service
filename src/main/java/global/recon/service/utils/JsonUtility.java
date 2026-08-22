package global.recon.service.utils;

import global.recon.service.service.InvalidRequestException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsonUtility {

    private final ObjectMapper objectMapper;

    public JsonUtility(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void streamRows(InputStream inputStream, RowCallback callback) throws Exception {
        streamRows(inputStream, null, callback);
    }

    public void streamRows(InputStream inputStream, String recordPath, RowCallback callback) throws Exception {
        JsonNode root = objectMapper.readTree(inputStream);
        if (root == null || root.isNull() || root.isMissingNode()) {
            throw new InvalidRequestException("JSON file is empty");
        }
        List<JsonNode> records = resolveRecords(root, recordPath);
        if (records.isEmpty()) {
            throw new InvalidRequestException("JSON list at "
                    + (blank(recordPath) ? "the root" : recordPath)
                    + " contained no records");
        }
        for (JsonNode record : records) {
            callback.accept(toCanonicalRow(record));
        }
    }

    List<JsonNode> resolveRecords(JsonNode root, String recordPath) {
        String path = recordPath == null ? "" : recordPath.trim();
        if (path.isEmpty()) {
            if (root.isArray()) {
                return copyArray(root);
            }
            JsonNode firstArray = findFirstArray(root);
            if (firstArray != null) {
                return copyArray(firstArray);
            }
            return List.of(root);
        }
        List<String> parts = splitPath(path);
        JsonNode current = root;
        int index = 0;
        while (index < parts.size()) {
            String part = parts.get(index);
            if (isArrayMarker(part) || current.isArray()) {
                if (!current.isArray()) {
                    throw new InvalidRequestException("JSON path '" + path + "' is not a list of records");
                }
                List<String> rest = isArrayMarker(part)
                        ? parts.subList(index + 1, parts.size())
                        : parts.subList(index, parts.size());
                return projectArray(current, rest, path);
            }
            if (!current.isObject()) {
                throw new InvalidRequestException(
                        "JSON path '" + path + "' was not found. Use the list to compare, for example tradeList or [].order");
            }
            current = current.get(part);
            if (current == null || current.isMissingNode() || current.isNull()) {
                throw new InvalidRequestException(
                        "JSON path '" + path + "' was not found. Use the list to compare, for example tradeList or [].order");
            }
            index++;
        }
        if (current.isArray()) {
            return copyArray(current);
        }
        throw new InvalidRequestException("JSON path '" + path + "' is not a list of records");
    }

    private List<JsonNode> projectArray(JsonNode array, List<String> fieldPath, String originalPath) {
        if (array.isEmpty()) {
            return List.of();
        }
        List<JsonNode> records = new ArrayList<>();
        int row = 0;
        for (JsonNode element : array) {
            JsonNode picked = pickFields(element, fieldPath, originalPath, row);
            if (picked.isArray()) {
                throw new InvalidRequestException(
                        "JSON path '" + originalPath + "' on list item " + row + " is another list. Pick the object to compare, for example order");
            }
            records.add(picked);
            row++;
        }
        return records;
    }

    private JsonNode pickFields(JsonNode element, List<String> fieldPath, String originalPath, int row) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            return element;
        }
        JsonNode current = element;
        for (String part : fieldPath) {
            if (isArrayMarker(part)) {
                throw new InvalidRequestException(
                        "JSON path '" + originalPath + "' cannot nest another list after item " + row);
            }
            if (current == null || !current.isObject()) {
                throw new InvalidRequestException(
                        "JSON path '" + originalPath + "' was not found on list item " + row);
            }
            current = current.get(part);
            if (current == null || current.isMissingNode() || current.isNull()) {
                throw new InvalidRequestException(
                        "JSON path '" + originalPath + "' was not found on list item " + row);
            }
        }
        return current;
    }

    private List<String> splitPath(String path) {
        String cleaned = path.replace('\\', '/').replaceFirst("^/+", "");
        List<String> parts = new ArrayList<>();
        for (String raw : cleaned.split("[./]")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String piece = raw.trim();
            int marker = piece.indexOf("[]");
            if (marker >= 0) {
                String before = piece.substring(0, marker).trim();
                if (!before.isEmpty()) {
                    parts.add(before);
                }
                parts.add("[]");
                String after = piece.substring(marker + 2).trim();
                if (!after.isEmpty()) {
                    parts.add(after);
                }
                continue;
            }
            if ("*".equals(piece)) {
                parts.add("[]");
                continue;
            }
            parts.add(piece);
        }
        return parts;
    }

    private boolean isArrayMarker(String part) {
        return "[]".equals(part) || "*".equals(part);
    }

    private List<JsonNode> copyArray(JsonNode array) {
        List<JsonNode> rows = new ArrayList<>();
        for (JsonNode element : array) {
            rows.add(element);
        }
        return rows;
    }

    private JsonNode findFirstArray(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = root.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue() != null && field.getValue().isArray()) {
                return field.getValue();
            }
        }
        return null;
    }

    private static final int MAX_FLATTEN_DEPTH = 12;
    private static final int MAX_FIELD_NAME = 255;

    public Map<String, Object> toCanonicalRow(JsonNode node) {
        Map<String, Object> row = new LinkedHashMap<>();
        flatten(node, "", 0, row);
        return row;
    }

    private void flatten(JsonNode node, String prefix, int depth, Map<String, Object> row) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            if (!prefix.isEmpty()) {
                putField(row, prefix, null);
            }
            return;
        }
        if (depth > MAX_FLATTEN_DEPTH) {
            throw new InvalidRequestException(
                    "JSON nesting is deeper than " + MAX_FLATTEN_DEPTH + " levels at '" + prefix + "'");
        }
        if (node.isObject()) {
            if (node.isEmpty()) {
                return;
            }
            for (var entry : node.properties()) {
                flatten(entry.getValue(), joinPath(prefix, entry.getKey()), depth + 1, row);
            }
            return;
        }
        if (node.isArray()) {
            int index = 0;
            for (JsonNode element : node) {
                flatten(element, joinPath(prefix, String.valueOf(index)), depth + 1, row);
                index++;
            }
            return;
        }
        if (prefix.isEmpty()) {
            putField(row, "value", jsonValue(node));
            return;
        }
        putField(row, prefix, jsonValue(node));
    }

    private String joinPath(String prefix, String name) {
        String safe = name == null ? "" : name.trim();
        if (safe.isEmpty()) {
            return prefix;
        }
        return prefix.isEmpty() ? safe : prefix + "." + safe;
    }

    private void putField(Map<String, Object> row, String key, Object value) {
        if (key.length() > MAX_FIELD_NAME) {
            throw new InvalidRequestException("Flattened field name exceeds " + MAX_FIELD_NAME + " characters: " + key);
        }
        if (row.containsKey(key)) {
            throw new InvalidRequestException("Flattened field '" + key + "' collided with another field of the same name");
        }
        row.put(key, value);
    }

    private Object jsonValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            if (node.isIntegralNumber()) {
                return node.longValue();
            }
            return node.decimalValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isTextual()) {
            String text = node.textValue();
            return text == null || text.isBlank() ? null : text.trim();
        }
        return node.toString();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
