package global.recon.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recon")
public class ReconProperties {

    private int chunkSize = 10_000;
    private int queueWorkers = 4;

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
}
