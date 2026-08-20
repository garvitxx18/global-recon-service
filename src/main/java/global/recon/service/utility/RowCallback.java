package global.recon.service.utility;

import java.util.Map;

@FunctionalInterface
public interface RowCallback {
    void accept(Map<String, Object> row) throws Exception;
}
