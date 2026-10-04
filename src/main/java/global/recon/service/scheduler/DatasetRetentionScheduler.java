package global.recon.service.scheduler;

import global.recon.service.config.ReconProperties;
import global.recon.service.service.DatasetRetentionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DatasetRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DatasetRetentionScheduler.class);

    private final ReconProperties reconProperties;
    private final DatasetRetentionService datasetRetentionService;

    public DatasetRetentionScheduler(
            ReconProperties reconProperties,
            DatasetRetentionService datasetRetentionService) {
        this.reconProperties = reconProperties;
        this.datasetRetentionService = datasetRetentionService;
    }

    @Scheduled(cron = "${recon.retention.cron:0 0 2 * * *}", zone = "${recon.retention.zone:Asia/Kolkata}")
    public void runAtNight() {
        if (!reconProperties.getRetention().isEnabled()) {
            return;
        }
        try {
            datasetRetentionService.purgeExpired();
        } catch (Exception ex) {
            log.error("Nightly dataset retention failed", ex);
        }
    }
}
