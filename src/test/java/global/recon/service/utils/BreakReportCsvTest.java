package global.recon.service.utils;

import global.recon.service.model.ReconResult;
import global.recon.service.model.ReconStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BreakReportCsvTest {

    @Test
    void writesOneRowPerDifferenceAndQuotesCommas() throws Exception {
        ReconResult result = new ReconResult();
        result.setStatus(ReconStatus.BREAK);
        result.setReconKey("T002");
        result.setDifferencesJson("""
                [
                  {"leftField":"price","rightField":"price","leftValue":"410.00","rightValue":"410.50"},
                  {"leftField":"note","rightField":"note","leftValue":"a,b","rightValue":"a,c"}
                ]
                """);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (var printer = BreakReportCsv.open(output)) {
            BreakReportCsv.writeResult(printer, result);
        }

        String csv = output.toString(StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFF");
        assertThat(csv).contains("status,recon_key,left_field,right_field,left_value,right_value");
        assertThat(csv).contains("BREAK,T002,price,price,410.00,410.50");
        assertThat(csv).contains("\"a,b\"");
        assertThat(csv).contains("\"a,c\"");
    }

    @Test
    void writesAKeyRowWhenThereAreNoDifferences() throws Exception {
        ReconResult result = new ReconResult();
        result.setStatus(ReconStatus.ONLY_IN_LEFT);
        result.setReconKey("T004");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (var printer = BreakReportCsv.open(output)) {
            BreakReportCsv.writeResult(printer, result);
        }

        String csv = output.toString(StandardCharsets.UTF_8);
        assertThat(csv).contains("ONLY_IN_LEFT,T004,,,,");
    }
}
