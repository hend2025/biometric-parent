package com.biometric.algo.service;

import com.biometric.algo.aggregator.FaceRecogAggregator;
import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.CompareParams;
import com.biometric.algo.dto.CompareResult;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 人脸识别服务
 * 提供人脸1:N搜索功能，基于Hazelcast聚合器实现
 * 
 * 主要功能：
 * - 1:N特征比对
 * - 组别过滤
 * - 结果排序与筛选
 * 
 * @author biometric-algo
 * @version 1.0
 */
@Service
public class FaceRecogService {
    private static final Logger log = LoggerFactory.getLogger(FaceRecogService.class);
    private final IMap<String, CachedFaceFeature> faceFeatureMap;

    @Autowired
    public FaceRecogService(FaceCacheService faceCacheService) {
        this.faceFeatureMap = faceCacheService.getFaceFeatureMap();
    }

    public List<CompareResult> recogOneToMany(CompareParams params) {
        if (params == null) {
            throw new IllegalArgumentException("RecogParam cannot be null");
        }
        if ((params.getFeatures() == null || CollectionUtils.isEmpty(params.getFeatures())) &&
                CollectionUtils.isEmpty(params.getImages())) {
            throw new IllegalArgumentException("Features cannot be null");
        }
        if (params.getThreshold() < 0 || params.getThreshold() > 1) {
            throw new IllegalArgumentException("Threshold must be between 0 and 1, got: " + params.getThreshold());
        }
        if (params.getTopN() < 1 || params.getTopN() > 100) {
            throw new IllegalArgumentException("topN must be between 1 and 100, got: " + params.getTopN());
        }

        List<CompareResult> result = null;
        long startTime = System.currentTimeMillis();
        FaceRecogAggregator aggregator = new FaceRecogAggregator(params);
        if(params.getGroups() == null || CollectionUtils.isEmpty(params.getGroups())){
            result = faceFeatureMap.aggregate(aggregator);
        }else{
            Predicate<String, CachedFaceFeature> groupPredicate = Predicates.in("groupIds[any]", params.getGroups().toArray(new String[0]));
            result = faceFeatureMap.aggregate(aggregator, groupPredicate);
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("1:N 搜索耗时: {}ms", totalDuration);
        
        return result;
    }

}