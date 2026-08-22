package global.recon.service.feignclient;

import global.recon.service.config.LlmProperties;
import global.recon.service.service.LlmDiscoveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient restClient;
    private final LlmProperties llmProperties;

    public GeminiClient(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;
        this.restClient = RestClient.builder()
                .baseUrl(llmProperties.getGemini().getBaseUrl())
                .build();
    }

    public String complete(String prompt) {
        if (!llmProperties.hasGeminiKey()) {
            throw new LlmDiscoveryException("Gemini API key is not configured");
        }
        try {
            String model = llmProperties.getGemini().getModel();
            String path = "/v1beta/models/" + model + ":generateContent";
            log.info("LLM request provider=gemini model={} path={} promptChars={}",
                    model, path, prompt == null ? 0 : prompt.length());
            log.info("LLM prompt:\n{}", prompt);
            GeminiGenerateResponse response = restClient.post()
                    .uri(path)
                    .header("x-goog-api-key", llmProperties.getGemini().getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(GeminiGenerateRequest.fromPrompt(prompt))
                    .retrieve()
                    .body(GeminiGenerateResponse.class);
            String text = response == null ? null : response.firstText();
            if (text == null || text.isBlank()) {
                throw new LlmDiscoveryException("Gemini returned an empty response");
            }
            log.info("LLM response chars={}:\n{}", text.length(), text);
            return text;
        } catch (LlmDiscoveryException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.error("LLM HTTP error status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new LlmDiscoveryException(
                    "Gemini API error: HTTP " + ex.getStatusCode().value() + " " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("LLM call failed: {}", ex.getMessage());
            throw new LlmDiscoveryException("Gemini API call failed: " + ex.getMessage(), ex);
        }
    }
}
