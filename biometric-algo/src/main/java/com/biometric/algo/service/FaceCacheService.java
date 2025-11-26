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

@Service
public class FaceCacheService {
    private static final Logger log = LoggerFactory.getLogger(FaceCacheService.class);

    // 增加批次大小以减少网络交互次数
    private static final int SUB_BATCH_SIZE = 5000;
    private final IMap<String, PersonFaceData> faceFeatureMap;

    @Autowired
    public FaceCacheService(HazelcastInstance hazelcastInstance) {
        this.faceFeatureMap = hazelcastInstance.getMap(HazelcastConfiguration.FACE_FEATURE_MAP);
    }

    public void loadFeatures(List<PersonFaceData> features) {
        if (features == null || features.isEmpty()) return;

        // 如果批次正好合适，直接转 Map 写入，避免 subList 创建的开销
        if (features.size() <= SUB_BATCH_SIZE) {
            putAllToHazelcast(features);
        } else {
            // 大批次拆分
            int totalSize = features.size();
            for (int i = 0; i < totalSize; i += SUB_BATCH_SIZE) {
                int end = Math.min(i + SUB_BATCH_SIZE, totalSize);
                putAllToHazelcast(features.subList(i, end));
            }
        }
    }

    private void putAllToHazelcast(List<PersonFaceData> batch) {
        try {
            // 指定 HashMap 大小，避免 resize 扩容开销
            Map<String, PersonFaceData> batchMap = new HashMap<>((int)(batch.size() / 0.75) + 1);

            for (PersonFaceData data : batch) {
                batchMap.put(data.getPersonId(), data);
            }

            // Hazelcast 的 putAll 是最高效的批量写入方式
            faceFeatureMap.putAll(batchMap);

        } catch (Exception e) {
            log.error("Hazelcast 批量写入失败", e);
        }
    }

    public void clearCache() {
        faceFeatureMap.clear();
    }

    public IMap<String, PersonFaceData> getFaceFeatureMap() {
        return faceFeatureMap;
    }

}