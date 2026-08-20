package global.recon.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReconPipelineIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadDiscoverApproveAndRun() throws Exception {
        String leftId = upload("left.csv", read("samples/left.csv"));
        String rightId = upload("right.csv", read("samples/right.csv"));

        MvcResult discover = mockMvc.perform(post("/api/v1/recon-plans/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leftDatasetId":"%s","rightDatasetId":"%s"}
                                """.formatted(leftId, rightId)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode plan = objectMapper.readTree(discover.getResponse().getContentAsString());
        String planId = plan.get("id").asString();
        assertThat(plan.get("keyMappings").isEmpty()).isFalse();

        mockMvc.perform(post("/api/v1/recon-plans/" + planId + "/approve"))
                .andExpect(status().isOk());

        MvcResult runResult = mockMvc.perform(post("/api/v1/recon-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reconPlanId":"%s"}
                                """.formatted(planId)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode run = objectMapper.readTree(runResult.getResponse().getContentAsString());
        String runId = run.get("id").asString();
        assertThat(run.get("status").asString()).isEqualTo("COMPLETED");
        assertThat(run.get("matchedCount").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(run.get("onlyInLeftCount").asLong()).isEqualTo(1);
        assertThat(run.get("onlyInRightCount").asLong()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/recon-runs/" + runId + "/summary"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/recon-runs/" + runId + "/results").param("status", "BREAK"))
                .andExpect(status().isOk());
    }

    private String upload(String filename, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "text/csv", content);
        MvcResult result = mockMvc.perform(multipart("/api/v1/datasets").file(file).param("name", filename))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    private byte[] read(String path) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).isNotNull();
            return stream.readAllBytes();
        }
    }
}
