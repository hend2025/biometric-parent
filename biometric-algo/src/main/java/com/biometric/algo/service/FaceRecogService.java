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
        long startTime = System.currentTimeMillis();
        int totalCandidates = faceFeatureMap.size();
        
        log.info("Starting 1:N Top-{} search | Groups: {} | Total candidates: {} | Threshold: {}", 
                topN, targetGroupIds != null ? targetGroupIds.size() : "ALL", totalCandidates, threshold);
        
        List<RecogResult> result = null;
        FaceRecogAggregator aggregator = new FaceRecogAggregator(inputFeature, threshold, topN);
        
        long aggregationStart = System.currentTimeMillis();
        if(targetGroupIds!=null && !targetGroupIds.isEmpty()){
            Predicate<String, CachedFaceFeature> groupPredicate = Predicates.in("groupIds[any]", targetGroupIds.toArray(new String[0]));
            result = faceFeatureMap.aggregate(aggregator, groupPredicate);
        }else{
            result = faceFeatureMap.aggregate(aggregator);
        }
        long aggregationDuration = System.currentTimeMillis() - aggregationStart;
        long totalDuration = System.currentTimeMillis() - startTime;
        
        log.info("1:N search completed | Total: {}ms | Aggregation: {}ms | Matches: {} | Throughput: {:.2f} comparisons/ms", 
                totalDuration, aggregationDuration, result.size(), 
                totalCandidates / (double) Math.max(aggregationDuration, 1));
        
        return result;
    }

}