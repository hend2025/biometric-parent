package com.biometric.algo.service;

import com.biometric.algo.config.HazelcastConfiguration;
import com.biometric.algo.dto.CachedFaceFeature;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FaceCacheService {
    private static final Logger log = LoggerFactory.getLogger(FaceCacheService.class);
    private final IMap<String, CachedFaceFeature> faceFeatureMap;

    @Autowired
    public FaceCacheService(HazelcastInstance hazelcastInstance) {
        this.faceFeatureMap = hazelcastInstance.getMap(HazelcastConfiguration.FACE_FEATURE_MAP);
    }

    public void loadFeatures(List<CachedFaceFeature> features) {
        if (features == null || features.isEmpty()) {
            return;
        }
        Map<String, CachedFaceFeature> batchMap = features.stream()
                .collect(Collectors.toMap(CachedFaceFeature::getFaceId, Function.identity()));
        faceFeatureMap.putAll(batchMap);
        log.info("Loaded {} features into Hazelcast cache.", batchMap.size());
    }

    public void clearCache() {
        log.warn("Clearing all features from Hazelcast cache...");
        faceFeatureMap.clear();
    }

    public IMap<String, CachedFaceFeature> getFaceFeatureMap() {
        return faceFeatureMap;
    }

}