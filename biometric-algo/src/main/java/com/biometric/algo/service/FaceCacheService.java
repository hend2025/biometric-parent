package com.biometric.algo.service;

import com.biometric.algo.config.HazelcastConfiguration;
import com.biometric.algo.dto.PersonFaceData;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Service
public class FaceCacheService {
    private static final Logger log = LoggerFactory.getLogger(FaceCacheService.class);

    private static final int SUB_BATCH_SIZE = 1000;
    private static final int MAX_CONCURRENT_WRITES = 4;
    private final Semaphore writeSemaphore = new Semaphore(MAX_CONCURRENT_WRITES);

    private final IMap<String, PersonFaceData> faceFeatureMap;

    @Autowired
    public FaceCacheService(HazelcastInstance hazelcastInstance) {
        this.faceFeatureMap = hazelcastInstance.getMap(HazelcastConfiguration.FACE_FEATURE_MAP);
    }

    public void loadFeatures(List<PersonFaceData> features) {
        if (features == null || features.isEmpty()) return;

        if (features.size() <= SUB_BATCH_SIZE) {
            putAllToHazelcast(features);
        } else {
            int totalSize = features.size();
            for (int i = 0; i < totalSize; i += SUB_BATCH_SIZE) {
                int end = Math.min(i + SUB_BATCH_SIZE, totalSize);
                putAllToHazelcast(features.subList(i, end));
            }
        }
    }

    private void putAllToHazelcast(List<PersonFaceData> batch) {
        try {
            // 获取许可，如果 Hazelcast 写入慢，这里会阻塞，从而降低 DataLoadService 的生产速度
            writeSemaphore.acquire();

            try {
                // 精确设置 Map 容量，避免 resize
                Map<String, PersonFaceData> batchMap = new HashMap<>((int)(batch.size() / 0.75) + 1);
                for (PersonFaceData data : batch) {
                    batchMap.put(data.getPersonId(), data);
                }
                faceFeatureMap.putAll(batchMap);
            } finally {
                writeSemaphore.release();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("缓存写入被中断");
        } catch (Exception e) {
            log.error("Hazelcast 批量写入异常", e);
        }
    }

    public void clearCache() {
        faceFeatureMap.clear();
    }

    public IMap<String, PersonFaceData> getFaceFeatureMap() {
        return faceFeatureMap;
    }

}