package com.biometric.algo.service;

import com.biometric.algo.aggregator.FaceRecogAggregator;
import com.biometric.algo.dto.CompareParams;
import com.biometric.algo.dto.CompareResult;
import com.biometric.algo.dto.PersonFaceData;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FaceRecogService {
    private static final Logger log = LoggerFactory.getLogger(FaceRecogService.class);
    private final IMap<String, PersonFaceData> faceFeatureMap;

    @Autowired
    public FaceRecogService(FaceCacheService faceCacheService) {
        this.faceFeatureMap = faceCacheService.getFaceFeatureMap();
    }

    public List<CompareResult> recogOneToMany(CompareParams params) {
        if (params == null) {
            throw new IllegalArgumentException("识别参数不能为空");
        }
        if ((params.getFeatures() == null || CollectionUtils.isEmpty(params.getFeatures())) &&
            (params.getImages() == null || CollectionUtils.isEmpty(params.getImages())) ) {
            throw new IllegalArgumentException("特征数据不能为空");
        }
        if (params.getThreshold() < 0 || params.getThreshold() > 1) {
            throw new IllegalArgumentException("阈值必须在 0 和 1 之间，当前值: " + params.getThreshold());
        }
        if (params.getTopN() < 1 || params.getTopN() > 100) {
            throw new IllegalArgumentException("topN 必须在 1 和 100 之间，当前值: " + params.getTopN());
        }

        List<CompareResult> result = null;
        long startTime = System.currentTimeMillis();
        FaceRecogAggregator aggregator = new FaceRecogAggregator(params);
        if(params.getGroups() == null || CollectionUtils.isEmpty(params.getGroups())){
            result = faceFeatureMap.aggregate(aggregator);
        }else{
            Predicate<String, PersonFaceData> groupPredicate = Predicates.in("groupIds[any]", params.getGroups().toArray(new String[0]));
            result = faceFeatureMap.aggregate(aggregator, groupPredicate);
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("1:N 搜索耗时: {}ms", totalDuration);
        
        return result;
    }

}