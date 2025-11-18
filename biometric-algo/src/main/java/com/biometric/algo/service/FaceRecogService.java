package com.biometric.algo.service;

import com.biometric.algo.aggregator.FaceRecogAggregator;
import com.biometric.algo.dto.CachedFaceFeature;
import com.biometric.algo.dto.RecogResult;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;

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

        FaceRecogAggregator aggregator = new FaceRecogAggregator(inputFeature, targetGroupIds, threshold, topN);

        Predicate<String, CachedFaceFeature> groupPredicate = entry -> {
            CachedFaceFeature feature = entry.getValue();
            if (feature.getGroupIds() == null || feature.getGroupIds().isEmpty()) {
                return false;
            }
            for (String targetGroupId : targetGroupIds) {
                if (feature.getGroupIds().contains(targetGroupId)) {
                    return true;
                }
            }
            return false;
        };

        List<RecogResult> result = faceFeatureMap.aggregate(aggregator, groupPredicate);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Search in groups {} finished in {} ms. Found: {} matches.", targetGroupIds, duration, result.size());
        return result;
    }

    public List<RecogResult> searchInAll(byte[] inputFeature, double threshold, int topN) {
        log.info("Starting 1:N Top-{} search in ALL database.", topN);
        long startTime = System.currentTimeMillis();
        FaceRecogAggregator aggregator = new FaceRecogAggregator(inputFeature, null, threshold, topN);
        List<RecogResult> result = faceFeatureMap.aggregate(aggregator);
        long duration = System.currentTimeMillis() - startTime;
        log.info("Search in ALL finished in {} ms. Found: {} matches.", duration, result.size());
        return result;
    }

}