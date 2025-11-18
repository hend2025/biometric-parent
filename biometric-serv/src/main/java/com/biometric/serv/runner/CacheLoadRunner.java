package com.biometric.serv.runner;

import com.biometric.algo.service.FaceCacheService;
import com.biometric.serv.service.DataLoadService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.lock.FencedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class CacheLoadRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CacheLoadRunner.class);
    private static final String LOAD_LOCK_NAME = "face-feature-load-lock";

    @Value("${face.feature.load.lock-timeout-seconds:300}")
    private int lockTimeoutSeconds;
    @Value("${biometric.load-on-startup:true}")
    private boolean loadOnStartup;

    @Autowired
    private DataLoadService dataLoadService;
    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private FaceCacheService faceCacheService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!loadOnStartup) {
            log.warn("CacheLoadRunner: `biometric.load-on-startup` is false. Skipping data load check.");
            return;
        }
        FencedLock lock = hazelcastInstance.getCPSubsystem().getLock(LOAD_LOCK_NAME);
        boolean lockAcquired = false;
        try {
            log.info("Attempting to acquire distributed lock for feature loading...");
            lockAcquired = lock.tryLock(lockTimeoutSeconds, TimeUnit.SECONDS);
            if (lockAcquired) {
                log.info("Distributed lock acquired. This node will load features.");
                if (faceCacheService.getFaceFeatureMap().isEmpty()) {
                    dataLoadService.loadAllFeaturesIntoCache();
                }
            }
        } catch (Exception e) {
            log.error("Error loading face features", e);
            throw e;
        } finally {
            if (lockAcquired) {
                lock.unlock();
                log.info("Distributed lock released");
            }
        }
    }

}