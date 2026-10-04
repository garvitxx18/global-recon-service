package global.recon.service.utils;

import java.util.UUID;

public final class IdUtility {

    private IdUtility() {
    }

    public static String datasetId() {
        return prefixed("DS");
    }

    public static String columnId() {
        return prefixed("DC");
    }

    public static String recordId() {
        return prefixed("DR");
    }

    public static String planId() {
        return prefixed("RP");
    }

    public static String keyMappingId() {
        return prefixed("KM");
    }

    public static String fieldMappingId() {
        return prefixed("FM");
    }

    public static String jobId() {
        return prefixed("JOB");
    }

    public static String runId() {
        return prefixed("RR");
    }

    public static String resultId() {
        return prefixed("RS");
    }

    public static String sourceId() {
        return prefixed("SRC");
    }

    public static String collectionId() {
        return prefixed("COL");
    }

    public static String collectionMemberId() {
        return prefixed("CM");
    }

    public static String collectionPairId() {
        return prefixed("CP");
    }

    public static String collectionCycleId() {
        return prefixed("CY");
    }

    public static String collectionItemId() {
        return prefixed("CI");
    }

    private static String prefixed(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
