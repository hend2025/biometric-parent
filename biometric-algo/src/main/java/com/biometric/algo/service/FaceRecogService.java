package com.biometric.algo.service;

import com.biometric.algo.aggregator.FaceRecogAggregator;
import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.RecogResult;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

@Service
public class FaceRecogService {
    private static final Logger log = LoggerFactory.getLogger(FaceRecogService.class);
    private final IMap<String, CachedFaceFeature> faceFeatureMap;

    @Autowired
    public FaceRecogService(FaceCacheService faceCacheService) {
        this.faceFeatureMap = faceCacheService.getFaceFeatureMap();
    }

    public List<RecogResult> searchInGroups(byte[] inputFeature, Set<String> targetGroupIds, double threshold, int topN) {
        log.info("Starting 1:N Top-{} search in groups: {}", topN, targetGroupIds);
        long startTime = System.currentTimeMillis();
        List<RecogResult> result = null;
        FaceRecogAggregator aggregator = new FaceRecogAggregator(inputFeature, targetGroupIds, threshold, topN);
        if(targetGroupIds!=null && !targetGroupIds.isEmpty()){
            Predicate<String, CachedFaceFeature> groupPredicate = Predicates.in("groupIds[any]", targetGroupIds.toArray(new String[0]));
            result = faceFeatureMap.aggregate(aggregator, groupPredicate);
        }else{
            result = faceFeatureMap.aggregate(aggregator);
        }
        long duration = System.currentTimeMillis() - startTime;
        log.info("Search in groups {} finished in {} ms. Found: {} matches.", targetGroupIds, duration, result.size());
        return result;
    }

}