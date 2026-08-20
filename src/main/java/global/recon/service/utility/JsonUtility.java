package global.recon.service.utility;

import org.springframework.stereotype.Component;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JsonUtility {

    private final ObjectMapper objectMapper;

    public JsonUtility(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void streamRows(InputStream inputStream, RowCallback callback) throws Exception {
        try (JsonParser parser = objectMapper.createParser(inputStream)) {
            JsonToken token = parser.nextToken();
            if (token == JsonToken.START_ARRAY) {
                streamArray(parser, callback);
                return;
            }
            if (token == JsonToken.START_OBJECT) {
                JsonNode root = objectMapper.readTree(parser);
                if (root == null || root.isNull()) {
                    throw new IllegalArgumentException("JSON file is empty");
                }
                JsonNode array = findFirstArray(root);
                if (array != null && array.isArray()) {
                    for (JsonNode element : array) {
                        callback.accept(toCanonicalRow(element));
                    }
                    return;
                }
                callback.accept(toCanonicalRow(root));
                return;
            }
            throw new IllegalArgumentException("JSON root must be an object or array");
        }
    }

    private void streamArray(JsonParser parser, RowCallback callback) throws Exception {
        JsonToken token = parser.nextToken();
        while (token != null && token != JsonToken.END_ARRAY) {
            JsonNode element = objectMapper.readTree(parser);
            callback.accept(toCanonicalRow(element));
            token = parser.nextToken();
        }
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

    public Map<String, Object> toCanonicalRow(JsonNode node) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (node == null || node.isNull()) {
            return row;
        }
        if (!node.isObject()) {
            row.put("value", jsonValue(node));
            return row;
        }
        node.properties().forEach(entry -> row.put(entry.getKey(), jsonValue(entry.getValue())));
        return row;
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
}
