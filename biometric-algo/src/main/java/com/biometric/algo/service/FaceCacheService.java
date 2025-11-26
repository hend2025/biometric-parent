package com.biometric.algo.service;

import com.biometric.algo.config.HazelcastConfiguration;
import com.biometric.algo.dto.PersonFaceData;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class FaceCacheService {
    private static final Logger log = LoggerFactory.getLogger(FaceCacheService.class);
    private static final int SUB_BATCH_SIZE = 2000;
    private final IMap<String, PersonFaceData> faceFeatureMap;

    @Autowired
    public FaceCacheService(HazelcastInstance hazelcastInstance) {
        this.faceFeatureMap = hazelcastInstance.getMap(HazelcastConfiguration.FACE_FEATURE_MAP);
    }

    public void loadFeatures(List<PersonFaceData> features) {
        if (features == null || features.isEmpty()) {
            return;
        }
        
        int totalSize = features.size();
        // 分批写入Hazelcast，避免单次写入过大导致内存峰值和Full GC
        if (totalSize <= SUB_BATCH_SIZE) {
            putAllToHazelcast(features);
        } else {
            for (int i = 0; i < totalSize; i += SUB_BATCH_SIZE) {
                int end = Math.min(i + SUB_BATCH_SIZE, totalSize);
                putAllToHazelcast(features.subList(i, end));
            }
        }
    }
    
    private void putAllToHazelcast(List<PersonFaceData> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        
        try {
            int capacity = (int) (batch.size() / 0.75f) + 1;
            Map<String, PersonFaceData> batchMap = new java.util.HashMap<>(capacity);
            
            // 手动构建Map，避免Stream的额外开销
            for (PersonFaceData data : batch) {
                batchMap.put(data.getPersonId(), data);
            }
            
            faceFeatureMap.putAll(batchMap);
            batchMap.clear();  // 显式清理，帮助GC
            
        } catch (Exception e) {
            log.error("批量写入Hazelcast失败，批次大小: {}", batch.size(), e);
            throw e;
        }
    }

    public void clearCache() {
        log.warn("正在清空Hazelcast缓存中的所有人脸特征...");
        faceFeatureMap.clear();
    }

    public IMap<String, PersonFaceData> getFaceFeatureMap() {
        return faceFeatureMap;
    }

}