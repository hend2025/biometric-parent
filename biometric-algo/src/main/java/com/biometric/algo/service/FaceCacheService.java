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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FaceCacheService {
    private static final Logger log = LoggerFactory.getLogger(FaceCacheService.class);
    private final IMap<String, PersonFaceData> faceFeatureMap;

    @Autowired
    public FaceCacheService(HazelcastInstance hazelcastInstance) {
        this.faceFeatureMap = hazelcastInstance.getMap(HazelcastConfiguration.FACE_FEATURE_MAP);
    }

    public void loadFeatures(List<PersonFaceData> features) {
        if (features == null || features.isEmpty()) {
            return;
        }
        Map<String, PersonFaceData> batchMap = features.stream()
                .collect(Collectors.toMap(PersonFaceData::getPersonId, Function.identity()));
        faceFeatureMap.putAll(batchMap);
        log.info("已加载 {} 条人员特征数据到Hazelcast缓存", batchMap.size());
    }

    public void clearCache() {
        log.warn("正在清空Hazelcast缓存中的所有人脸特征...");
        faceFeatureMap.clear();
    }

    public IMap<String, PersonFaceData> getFaceFeatureMap() {
        return faceFeatureMap;
    }

}