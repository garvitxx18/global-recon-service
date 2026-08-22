package global.recon.service.scheduler;

import global.recon.service.config.HazelcastConfig;
import global.recon.service.config.ReconProperties;
import global.recon.service.service.JobQueueService;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class JobWorker implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    private final HazelcastInstance hazelcastInstance;
    private final JobQueueService jobQueueService;
    private final ReconProperties reconProperties;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;

    public JobWorker(
            HazelcastInstance hazelcastInstance,
            JobQueueService jobQueueService,
            ReconProperties reconProperties) {
        this.hazelcastInstance = hazelcastInstance;
        this.jobQueueService = jobQueueService;
        this.reconProperties = reconProperties;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        jobQueueService.recoverQueuedJobs();
        int workers = Math.max(1, reconProperties.getQueueWorkers());
        executor = Executors.newFixedThreadPool(workers, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("recon-job-worker");
            thread.setDaemon(true);
            return thread;
        });
        for (int i = 0; i < workers; i++) {
            executor.submit(this::consume);
        }
        log.info("Started {} recon job workers on Hazelcast queue {}", workers, HazelcastConfig.JOB_QUEUE);
    }

    @Override
    public void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void consume() {
        IQueue<String> queue = hazelcastInstance.getQueue(HazelcastConfig.JOB_QUEUE);
        while (running.get()) {
            try {
                String jobId = queue.poll(2, TimeUnit.SECONDS);
                if (jobId != null) {
                    jobQueueService.process(jobId);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (HazelcastInstanceNotActiveException ex) {
                return;
            } catch (Exception ex) {
                if (!running.get()) {
                    return;
                }
                log.error("Job worker failed", ex);
            }
        }
    }
}
