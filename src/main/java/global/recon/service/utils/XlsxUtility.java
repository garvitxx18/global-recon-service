package global.recon.service.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class XlsxUtility {

    private final DataFormatter formatter = new DataFormatter();

    public void streamRows(InputStream inputStream, RowCallback callback) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("XLSX workbook has no sheets");
            }
            Iterator<Row> rows = sheet.rowIterator();
            if (!rows.hasNext()) {
                throw new IllegalArgumentException("XLSX sheet is empty");
            }
            List<String> headers = readHeaders(rows.next());
            while (rows.hasNext()) {
                Row row = rows.next();
                if (isEmpty(row)) {
                    continue;
                }
                Map<String, Object> canonical = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    canonical.put(headers.get(i), cellValue(row.getCell(i)));
                }
                callback.accept(canonical);
            }
        }
    }

    private List<String> readHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        short last = headerRow.getLastCellNum();
        for (int i = 0; i < last; i++) {
            String header = formatter.formatCellValue(headerRow.getCell(i)).trim();
            if (header.isEmpty()) {
                header = "column_" + (i + 1);
            }
            headers.add(header);
        }
        if (headers.isEmpty()) {
            throw new IllegalArgumentException("XLSX sheet has no header row");
        }
        return headers;
    }

    private boolean isEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private Object cellValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return LocalDate.ofInstant(cell.getDateCellValue().toInstant(), ZoneId.systemDefault()).toString();
            }
            double numeric = cell.getNumericCellValue();
            if (numeric == Math.rint(numeric) && !Double.isInfinite(numeric)) {
                return (long) numeric;
            }
            return numeric;
        }
        if (cell.getCellType() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        }
        if (cell.getCellType() == CellType.FORMULA) {
            String formatted = formatter.formatCellValue(cell).trim();
            return formatted.isEmpty() ? null : formatted;
        }
        String text = formatter.formatCellValue(cell).trim();
        return text.isEmpty() ? null : text;
    }
}
