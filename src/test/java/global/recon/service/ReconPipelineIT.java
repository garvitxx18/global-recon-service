package global.recon.service;

import global.recon.service.model.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReconPipelineIT {

    private static final String USER = "recon.tester@company.com";
    private static final String OTHER = "other.user@company.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadDiscoverApproveAndRun() throws Exception {
        String leftId = upload("left.csv", read("samples/left.csv"), "text/csv", USER, null, null);
        String rightId = upload("right.csv", read("samples/right.csv"), "text/csv", USER, null, null);

        MvcResult discover = mockMvc.perform(post("/api/v1/recon-plans/discover")
                        .with(asUser(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leftDatasetId":"%s","rightDatasetId":"%s","notes":"Compare trade rows only."}
                                """.formatted(leftId, rightId)))
                .andExpect(status().isAccepted())
                .andReturn();
        JsonNode discoverJob = waitForJob(objectMapper.readTree(discover.getResponse().getContentAsString()).get("id").asString(), USER);
        String planId = discoverJob.get("resultId").asString();
        assertThat(planId).isNotBlank();

        JsonNode plan = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/recon-plans/" + planId).with(asUser(USER)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        assertThat(plan.get("keyMappings").isEmpty()).isFalse();
        assertThat(plan.get("fieldMappings").isEmpty()).isFalse();
        assertThat(plan.get("ownerEmail").asString()).isEqualTo(USER);
        assertThat(plan.get("userNotes").asString()).isEqualTo("Compare trade rows only.");

        mockMvc.perform(post("/api/v1/recon-plans/" + planId + "/approve").with(asUser(USER)))
                .andExpect(status().isOk());

        MvcResult runSubmit = mockMvc.perform(post("/api/v1/recon-runs")
                        .with(asUser(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reconPlanId":"%s"}
                                """.formatted(planId)))
                .andExpect(status().isAccepted())
                .andReturn();
        JsonNode runJob = waitForJob(objectMapper.readTree(runSubmit.getResponse().getContentAsString()).get("id").asString(), USER);
        String runId = runJob.get("resultId").asString();

        JsonNode run = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/recon-runs/" + runId).with(asUser(USER)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        assertThat(run.get("status").asString()).isEqualTo("COMPLETED");
        assertThat(run.get("ownerEmail").asString()).isEqualTo(USER);
        assertThat(run.get("matchedCount").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(run.get("onlyInLeftCount").asLong()).isEqualTo(1);
        assertThat(run.get("onlyInRightCount").asLong()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/recon-runs/" + runId + "/summary").with(asUser(USER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/recon-runs/" + runId + "/results").param("status", "BREAK").with(asUser(USER)))
                .andExpect(status().isOk());

        MvcResult exportStart = mockMvc.perform(get("/api/v1/recon-runs/" + runId + "/export").with(asUser(USER)))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult export = mockMvc.perform(asyncDispatch(exportStart))
                .andExpect(status().isOk())
                .andReturn();
        String csv = export.getResponse().getContentAsString();
        assertThat(export.getResponse().getContentType()).contains("text/csv");
        assertThat(export.getResponse().getHeader("Content-Disposition")).contains("break-report-");
        assertThat(csv).contains("status,recon_key,left_field,right_field,left_value,right_value");
        assertThat(csv).contains("BREAK");
        mockMvc.perform(get("/api/v1/recon-runs/" + runId + "/export").with(asUser(OTHER)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/datasets/" + leftId + "/records").with(asUser(USER)))
                        .andExpect(status().isOk());

        JsonNode saved = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/comparisons")
                                .with(asUser(USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"runId":"%s","name":"sample left vs right"}
                                        """.formatted(runId)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        assertThat(saved.get("name").asString()).isEqualTo("sample left vs right");
        assertThat(saved.get("savedAt").isNull()).isFalse();

        JsonNode comparisons = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/comparisons").param("saved", "true").with(asUser(USER)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        assertThat(comparisons.get("content").isArray()).isTrue();
        assertThat(comparisons.get("content")).isNotEmpty();
    }

    @Test
    void nestedJsonUsesRecordPath() throws Exception {
        String datasetId = upload(
                "nested-left.json",
                read("samples/nested-left.json"),
                "application/json",
                USER,
                "Ignore asOf/book header singles and securityList. Ingest tradeList only.",
                "tradeList");
        JsonNode dataset = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/datasets/" + datasetId).with(asUser(USER)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        assertThat(dataset.get("rowCount").asLong()).isEqualTo(2);
        assertThat(dataset.get("recordPath").asString()).isEqualTo("tradeList");
        assertThat(dataset.get("ingestionNotes").asString()).contains("tradeList");
        assertThat(dataset.get("ownerEmail").asString()).isEqualTo(USER);

        JsonNode records = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/datasets/" + datasetId + "/records").with(asUser(USER)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        JsonNode first = records.get("content").get(0).get("payload");
        assertThat(first.get("trade_id").asString()).isEqualTo("T001");
        assertThat(first.get("isin")).isNull();
    }

    @Test
    void usersCannotSeeEachOthersDatasets() throws Exception {
        String datasetId = upload("left.csv", read("samples/left.csv"), "text/csv", USER, null, null);
        mockMvc.perform(get("/api/v1/datasets/" + datasetId).with(asUser(OTHER)))
                .andExpect(status().isNotFound());
        JsonNode otherList = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/datasets").with(asUser(OTHER)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        assertThat(otherList.toString()).doesNotContain(datasetId);
    }

    @Test
    void apiRequiresEmailHeader() throws Exception {
        mockMvc.perform(get("/api/v1/datasets"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode waitForJob(String jobId, String email) throws Exception {
        for (int i = 0; i < 50; i++) {
            JsonNode job = objectMapper.readTree(
                    mockMvc.perform(get("/api/v1/jobs/" + jobId).with(asUser(email)))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString());
            String status = job.get("status").asString();
            if (JobStatus.COMPLETED.name().equals(status)) {
                return job;
            }
            if (JobStatus.FAILED.name().equals(status)) {
                throw new AssertionError("Job failed: " + job.get("errorMessage"));
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for job " + jobId);
    }

    private String upload(
            String filename,
            byte[] content,
            String contentType,
            String email,
            String notes,
            String recordPath) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, content);
        var request = multipart("/api/v1/datasets").file(file).param("name", filename);
        if (notes != null) {
            request = request.param("notes", notes);
        }
        if (recordPath != null) {
            request = request.param("recordPath", recordPath);
        }
        MvcResult result = mockMvc.perform(request.with(asUser(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    private RequestPostProcessor asUser(String email) {
        return request -> {
            request.addHeader("X-User-Email", email);
            return request;
        };
    }

    private byte[] read(String path) throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).isNotNull();
            return stream.readAllBytes();
        }
    }
}
