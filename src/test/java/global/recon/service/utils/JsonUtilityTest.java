package global.recon.service.utils;

import global.recon.service.service.InvalidRequestException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonUtilityTest {

    private final JsonUtility jsonUtility = new JsonUtility(JsonMapper.builder().build());

    @Test
    void streamsNamedListAndIgnoresSiblingArrays() throws Exception {
        String json = """
                {
                  "asOf": "2026-08-20",
                  "securityList": [{ "isin": "US0378331005" }],
                  "tradeList": [
                    { "trade_id": "T001", "quantity": 100 },
                    { "trade_id": "T002", "quantity": 500 }
                  ]
                }
                """;
        List<Map<String, Object>> rows = new ArrayList<>();
        jsonUtility.streamRows(input(json), "tradeList", rows::add);
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst()).containsEntry("trade_id", "T001");
        assertThat(rows.getFirst()).doesNotContainKey("isin");
    }

    @Test
    void nestedPathUsesDotNotation() throws Exception {
        String json = """
                { "data": { "trades": [{ "id": "1" }] } }
                """;
        List<Map<String, Object>> rows = new ArrayList<>();
        jsonUtility.streamRows(input(json), "data.trades", rows::add);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("id", "1");
    }

    @Test
    void missingPathFailsClearly() {
        assertThatThrownBy(() -> jsonUtility.streamRows(input("{\"tradeList\":[]}"), "missing", row -> {}))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void emptyNamedListFails() {
        assertThatThrownBy(() -> jsonUtility.streamRows(input("{\"tradeList\":[]}"), "tradeList", row -> {}))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("no records");
    }

    @Test
    void picksNestedObjectFromEachRootListItem() throws Exception {
        String json = """
                [
                  {
                    "messageHeader": { "origin": "OrderManagementSystem", "eventId": "EVT-8017-A" },
                    "order": {
                      "orderReference": "TRD-A91X",
                      "securityId": "SEC-1001",
                      "executedQuantity": 120,
                      "executionPrice": 221.50
                    }
                  },
                  {
                    "messageHeader": { "origin": "OrderManagementSystem", "eventId": "EVT-8018-B" },
                    "order": {
                      "orderReference": "TRD-B02Y",
                      "securityId": "SEC-1002",
                      "executedQuantity": 10,
                      "executionPrice": 99.10
                    }
                  }
                ]
                """;
        List<Map<String, Object>> rows = new ArrayList<>();
        jsonUtility.streamRows(input(json), "order", rows::add);
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst())
                .containsEntry("orderReference", "TRD-A91X")
                .containsEntry("securityId", "SEC-1001")
                .doesNotContainKey("messageHeader")
                .doesNotContainKey("eventId");
        assertThat(rows.get(1)).containsEntry("orderReference", "TRD-B02Y");
    }

    @Test
    void bracketPathAlsoPicksNestedObject() throws Exception {
        String json = """
                [{ "messageHeader": { "eventId": "E1" }, "order": { "orderReference": "T1" } }]
                """;
        List<Map<String, Object>> rows = new ArrayList<>();
        jsonUtility.streamRows(input(json), "[].order", rows::add);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("orderReference", "T1");
    }

    @Test
    void namedListThenNestedObject() throws Exception {
        String json = """
                { "events": [{ "order": { "orderReference": "T1" } }] }
                """;
        List<Map<String, Object>> rows = new ArrayList<>();
        jsonUtility.streamRows(input(json), "events.order", rows::add);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst()).containsEntry("orderReference", "T1");
    }

    @Test
    void flattensNestedObjectsToDottedKeys() throws Exception {
        String json = """
                [{ "a": { "c": { "d": 23 } }, "name": "row1" }]
                """;
        List<Map<String, Object>> rows = new ArrayList<>();
        jsonUtility.streamRows(input(json), null, rows::add);
        assertThat(rows.getFirst())
                .containsEntry("a.c.d", 23L)
                .containsEntry("name", "row1")
                .doesNotContainKey("a");
    }

    @Test
    void flattensAfterPickingANestedRecord() throws Exception {
        String json = """
                [{ "order": { "ref": "T1", "fill": { "qty": 120, "price": 221.50 } } }]
                """;
        List<Map<String, Object>> rows = new ArrayList<>();
        jsonUtility.streamRows(input(json), "order", rows::add);
        assertThat(rows.getFirst())
                .containsEntry("ref", "T1")
                .containsEntry("fill.qty", 120L)
                .doesNotContainKey("order")
                .doesNotContainKey("fill");
    }

    @Test
    void wrapperWithoutPathBecomesDottedColumns() throws Exception {
        String json = """
                [{
                  "messageHeader": { "eventId": "EVT-8017-A" },
                  "order": { "orderReference": "TRD-A91X" }
                }]
                """;
        List<Map<String, Object>> rows = new ArrayList<>();
        jsonUtility.streamRows(input(json), null, rows::add);
        assertThat(rows.getFirst())
                .containsEntry("messageHeader.eventId", "EVT-8017-A")
                .containsEntry("order.orderReference", "TRD-A91X");
    }

    private ByteArrayInputStream input(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
