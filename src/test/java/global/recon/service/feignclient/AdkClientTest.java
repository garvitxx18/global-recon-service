package global.recon.service.feignclient;

import global.recon.service.service.LlmDiscoveryException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdkClientTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void extractsLastModelText() throws Exception {
        JsonNode events = mapper.readTree("""
                [
                  {"content":{"parts":[{"text":"thinking"}]}},
                  {"content":{"parts":[{"functionCall":{"name":"noop"}}]}},
                  {"content":{"parts":[{"text":"{\\"keyMappings\\":[]}"}]}}
                ]
                """);
        assertThat(AdkClient.extractText(events)).isEqualTo("{\"keyMappings\":[]}");
    }

    @Test
    void wrapsAgentErrorMessage() throws Exception {
        JsonNode events = mapper.readTree("""
                [{"errorMessage":"Vertex quota exceeded"}]
                """);
        assertThatThrownBy(() -> AdkClient.extractText(events))
                .isInstanceOf(LlmDiscoveryException.class)
                .hasMessageContaining("Vertex quota exceeded");
    }
}
