package com.biometric.serv.runner;

import com.biometric.algo.service.FaceCacheService;
import com.biometric.serv.service.DataLoadService;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class CacheLoadRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CacheLoadRunner.class);

    @Value("${biometric.face-loader.load-on-startup:true}")
    private boolean loadOnStartup;

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

        Member localMember = hazelcastInstance.getCluster().getLocalMember();
        List<Member> members = new ArrayList<>(hazelcastInstance.getCluster().getMembers());
        members.sort(Comparator.comparing(Member::getUuid));

        int totalShards = members.size();
        int shardIndex = members.indexOf(localMember);

        if (shardIndex == -1) {
            throw new IllegalStateException("Local member not found in cluster members list!");
        }

        log.info("This node is shard {} of {}", shardIndex, totalShards);

        long startTime = System.currentTimeMillis();
        dataLoadService.loadAllFeaturesIntoCache(shardIndex, totalShards);
        long duration = System.currentTimeMillis() - startTime;

        int finalSize = faceCacheService.getFaceFeatureMap().size();
        log.info("Cache load completed in {} ms. Final global cache size: {} features", duration, finalSize);

    }

}