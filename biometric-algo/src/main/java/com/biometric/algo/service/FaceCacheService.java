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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 人脸特征缓存服务
 * 使用Hazelcast分布式缓存管理加载的人脸特征数据
 * 
 * 主要功能：
 * - 批量加载特征到缓存
 * - 清空缓存
 * - 获取缓存Map实例
 * 
 * @author biometric-algo
 * @version 1.0
 */
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
        log.info("已加载 {} 条人脸特征到Hazelcast缓存", batchMap.size());
    }

    public void clearCache() {
        log.warn("正在清空Hazelcast缓存中的所有人脸特征...");
        faceFeatureMap.clear();
    }

    public IMap<String, CachedFaceFeature> getFaceFeatureMap() {
        return faceFeatureMap;
    }
}