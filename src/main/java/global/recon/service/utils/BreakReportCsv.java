package global.recon.service.utils;

import global.recon.service.model.ReconDifference;
import global.recon.service.model.ReconResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class BreakReportCsv {

    public static final String[] HEADERS = {
            "status",
            "recon_key",
            "left_field",
            "right_field",
            "left_value",
            "right_value"
    };

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader(HEADERS)
            .setRecordSeparator("\r\n")
            .get();

    private BreakReportCsv() {
    }

    public static CSVPrinter open(OutputStream output) throws IOException {
        output.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        return new CSVPrinter(writer, FORMAT);
    }

    public static void writeResult(CSVPrinter printer, ReconResult result) throws IOException {
        String status = result.getStatus() == null ? "" : result.getStatus().name();
        String key = nullToEmpty(result.getReconKey());
        List<ReconDifference> differences = result.getDifferences();
        if (differences == null || differences.isEmpty()) {
            printer.printRecord(status, key, "", "", "", "");
            return;
        }
        for (ReconDifference difference : differences) {
            printer.printRecord(
                    status,
                    key,
                    nullToEmpty(difference.getLeftField()),
                    nullToEmpty(difference.getRightField()),
                    nullToEmpty(difference.getLeftValue()),
                    nullToEmpty(difference.getRightValue()));
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
