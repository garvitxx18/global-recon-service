package global.recon.service.feignclient;

import global.recon.service.config.LlmProperties;
import global.recon.service.config.UserContext;
import global.recon.service.service.LlmDiscoveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AdkClient {

    private static final Logger log = LoggerFactory.getLogger(AdkClient.class);

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    public AdkClient(LlmProperties llmProperties, ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
    }

    public String complete(String prompt) {
        if (!llmProperties.hasAgentUrl()) {
            throw new LlmDiscoveryException("Mapping agent URL is not configured");
        }
        String appName = llmProperties.getAgent().getAppName();
        String userId = userId();
        String baseUrl = llmProperties.getAgent().getBaseUrl().replaceAll("/+$", "");
        RestClient restClient = restClient(baseUrl);
        try {
            String sessionId = createSession(restClient, appName, userId);
            log.info("LLM request provider=agent app={} session={} promptChars={}",
                    appName, sessionId, prompt == null ? 0 : prompt.length());
            log.info("LLM prompt:\n{}", prompt);
            String raw = restClient.post()
                    .uri("/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(runRequest(appName, userId, sessionId, prompt))
                    .retrieve()
                    .body(String.class);
            String text = extractText(parseEvents(raw));
            if (text == null || text.isBlank()) {
                throw new LlmDiscoveryException("Mapping agent returned an empty response");
            }
            log.info("LLM response chars={}:\n{}", text.length(), text);
            return text;
        } catch (LlmDiscoveryException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.error("LLM HTTP error status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new LlmDiscoveryException(
                    "Mapping agent error: HTTP " + ex.getStatusCode().value() + " " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("LLM call failed: {}", ex.getMessage());
            throw new LlmDiscoveryException("Mapping agent call failed: " + ex.getMessage(), ex);
        }
    }

    static String extractText(JsonNode events) {
        if (events == null || events.isNull() || events.isMissingNode()) {
            return null;
        }
        JsonNode array = events.isArray() ? events : events.path("events");
        if (!array.isArray()) {
            return textFromEvent(events);
        }
        String last = null;
        for (JsonNode event : array) {
            String error = textOrNull(event.path("errorMessage"));
            if (error != null) {
                throw new LlmDiscoveryException("Mapping agent error: " + error);
            }
            String text = textFromEvent(event);
            if (text != null && !text.isBlank()) {
                last = text;
            }
        }
        return last;
    }

    private String createSession(RestClient restClient, String appName, String userId) {
        String raw = restClient.post()
                .uri("/apps/{app}/users/{user}/sessions", appName, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .body(String.class);
        JsonNode session = parseEvents(raw);
        String sessionId = firstNonBlank(
                textOrNull(session.path("id")),
                textOrNull(session.path("sessionId")));
        if (sessionId == null) {
            throw new LlmDiscoveryException("Mapping agent did not return a session id");
        }
        return sessionId;
    }

    private Map<String, Object> runRequest(String appName, String userId, String sessionId, String prompt) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("text", prompt == null ? "" : prompt);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("parts", List.of(part));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appName", appName);
        body.put("userId", userId);
        body.put("sessionId", sessionId);
        body.put("newMessage", message);
        return body;
    }

    private JsonNode parseEvents(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LlmDiscoveryException("Mapping agent returned an empty response");
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            throw new LlmDiscoveryException("Mapping agent returned malformed JSON: " + ex.getMessage(), ex);
        }
    }

    private RestClient restClient(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(90));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    private String userId() {
        String email = UserContext.get();
        if (email != null && !email.isBlank()) {
            return email;
        }
        return "mapping-" + UUID.randomUUID();
    }

    private static String textFromEvent(JsonNode event) {
        JsonNode parts = event.path("content").path("parts");
        if (!parts.isArray()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.path("functionCall").isObject()) {
                continue;
            }
            String value = textOrNull(part.path("text"));
            if (value != null) {
                if (!text.isEmpty()) {
                    text.append('\n');
                }
                text.append(value);
            }
        }
        return text.isEmpty() ? null : text.toString();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String value = node.asString("");
        return value == null || value.isBlank() ? null : value.trim();
    }
}
