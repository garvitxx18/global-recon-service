package global.recon.service.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.config.QueueConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

    public static final String DATASET_PROFILE_MAP = "dataset-profile";
    public static final String RECON_PLAN_MAP = "recon-plan";
    public static final String RECON_PROGRESS_MAP = "recon-progress";
    public static final String JOB_QUEUE = "recon-jobs";

    @Bean
    public Config hazelcastInstanceConfig() {
        Config config = new Config();
        config.setClusterName("global-recon");
        config.setInstanceName("global-recon-hazelcast");
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        config.addMapConfig(ttlMap(DATASET_PROFILE_MAP, 3600));
        config.addMapConfig(ttlMap(RECON_PLAN_MAP, 3600));
        config.addMapConfig(ttlMap(RECON_PROGRESS_MAP, 7200));
        config.addMapConfig(ttlMap("recon-index-*", 7200));
        QueueConfig jobQueue = new QueueConfig(JOB_QUEUE);
        jobQueue.setBackupCount(1);
        jobQueue.setMaxSize(100_000);
        config.addQueueConfig(jobQueue);
        return config;
    }

    private MapConfig ttlMap(String name, int ttlSeconds) {
        MapConfig mapConfig = new MapConfig(name);
        mapConfig.setTimeToLiveSeconds(ttlSeconds);
        mapConfig.setMaxIdleSeconds(ttlSeconds);
        mapConfig.getEvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizePolicy.USED_HEAP_PERCENTAGE)
                .setSize(25);
        return mapConfig;
    }
}
