package global.recon.service.utils;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class JsonCodec {

    private final ObjectMapper objectMapper;

    public JsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        return objectMapper.readValue(json, Map.class);
    }

    public List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    public <T> List<T> readList(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }
}
