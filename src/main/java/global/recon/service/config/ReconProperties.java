package global.recon.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recon")
public class ReconProperties {

    private int chunkSize = 10_000;
    private int queueWorkers = 4;
    private Retention retention = new Retention();
    private Sources sources = new Sources();

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getQueueWorkers() {
        return queueWorkers;
    }

    public void setQueueWorkers(int queueWorkers) {
        this.queueWorkers = queueWorkers;
    }

    public Retention getRetention() {
        return retention;
    }

    public void setRetention(Retention retention) {
        this.retention = retention;
    }

    public Sources getSources() {
        return sources;
    }

    public void setSources(Sources sources) {
        this.sources = sources;
    }

    public static class Sources {
        private java.util.List<String> allowedHostSuffixes = new java.util.ArrayList<>();
        private boolean allowHttp = false;
        private long maxBodyBytes = 20 * 1024 * 1024;
        private int defaultMaxConcurrent = 2;
        private int connectTimeoutMs = 10_000;
        private int readTimeoutMs = 90_000;

        public java.util.List<String> getAllowedHostSuffixes() {
            return allowedHostSuffixes;
        }

        public void setAllowedHostSuffixes(java.util.List<String> allowedHostSuffixes) {
            this.allowedHostSuffixes = allowedHostSuffixes == null ? new java.util.ArrayList<>() : allowedHostSuffixes;
        }

        public boolean isAllowHttp() {
            return allowHttp;
        }

        public void setAllowHttp(boolean allowHttp) {
            this.allowHttp = allowHttp;
        }

        public long getMaxBodyBytes() {
            return maxBodyBytes;
        }

        public void setMaxBodyBytes(long maxBodyBytes) {
            this.maxBodyBytes = maxBodyBytes;
        }

        public int getDefaultMaxConcurrent() {
            return defaultMaxConcurrent;
        }

        public void setDefaultMaxConcurrent(int defaultMaxConcurrent) {
            this.defaultMaxConcurrent = defaultMaxConcurrent;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    public static class Retention {
        private boolean enabled = true;
        private int keepDays = 7;
        private String cron = "0 0 2 * * *";
        private String zone = "Asia/Kolkata";
        private int batchSize = 50;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getKeepDays() {
            return keepDays;
        }

        public void setKeepDays(int keepDays) {
            this.keepDays = keepDays;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
}
