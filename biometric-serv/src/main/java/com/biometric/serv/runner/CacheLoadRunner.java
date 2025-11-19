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

    @Value("${biometric.face-loader.load-on-startup:true}")
    private boolean loadOnStartup;
    @Value("${biometric.face-loader.lock-timeout-seconds:60}")
    private int lockTimeoutSeconds;
    @Value("${biometric.face-loader.force-reload:false}")
    private boolean forceReload;

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private DataLoadService dataLoadService;
    @Autowired
    private FaceCacheService faceCacheService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!loadOnStartup) {
            log.warn("CacheLoadRunner: `biometric.load-on-startup` is false. Skipping data load check.");
            return;
        }
        
        log.info("Starting cache load process. Lock timeout: {} seconds, Force reload: {}", lockTimeoutSeconds, forceReload);
        
        FencedLock lock = null;
        boolean lockAcquired = false;
        
        try {
            lock = hazelcastInstance.getCPSubsystem().getLock(LOAD_LOCK_NAME);
            
            log.info("Attempting to acquire distributed lock for feature loading...");
            lockAcquired = lock.tryLock(lockTimeoutSeconds, TimeUnit.SECONDS);
            
            if (!lockAcquired) {
                log.warn("Failed to acquire distributed lock within {} seconds. " +
                        "Another node may be loading data or lock is stuck.", lockTimeoutSeconds);
                return;
            }
            
            log.info("Distributed lock acquired. This node will load features.");
            
            int currentSize = faceCacheService.getFaceFeatureMap().size();
            log.info("Current cache size: {} features", currentSize);
            
            if (forceReload || currentSize == 0) {
                if (forceReload) {
                    log.info("Force reload enabled. Reloading all features...");
                } else {
                    log.info("Cache is empty. Loading features...");
                }
                
                long startTime = System.currentTimeMillis();
                dataLoadService.loadAllFeaturesIntoCache();
                long duration = System.currentTimeMillis() - startTime;
                
                int finalSize = faceCacheService.getFaceFeatureMap().size();
                log.info("Cache load completed in {} ms. Final cache size: {} features", duration, finalSize);
                        
                if (finalSize == 0) {
                    log.error("WARNING: Cache is still empty after loading. Check database connectivity.");
                }
            } else {
                log.info("Cache already contains {} features. Skipping reload.", currentSize);
            }

        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.error("Error loading face features", e);
            throw new RuntimeException("Failed to load face features", e);
        } finally {
            if (lockAcquired && lock != null) {
                try {
                    lock.unlock();
                    log.info("Distributed lock released");
                } catch (Exception e) {
                    log.error("Error releasing distributed lock", e);
                }
            }
        }
    }

}