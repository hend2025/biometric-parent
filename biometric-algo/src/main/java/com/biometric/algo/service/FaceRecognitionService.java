package com.biometric.algo.service;

import com.biometric.algo.aggregator.TopNFaceAggregator;
import com.biometric.algo.dto.FaceRecognitionDTO;
import com.biometric.algo.model.FaceFeature;
import com.biometric.algo.model.FaceMatchResult;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FaceRecognitionService {

    @Autowired
    @Qualifier("hazelcastInstance")
    private HazelcastInstance hazelcastInstance;

    @Value("${biometric.face.recognition.threshold:0.6}")
    private float matchThreshold;

    @Value("${biometric.face.recognition.topN:10}")
    private int topN;

    private static final String FACE_FEATURE_MAP = "faceFeatureMap";

    public String addFaceFeature(String psnNo, byte[] featureVector, String imageUrl) {
        String faceId = UUID.randomUUID().toString();
        return addFaceFeatureWithId(faceId, psnNo, featureVector, imageUrl);
    }

    public String addFaceFeatureWithId(String faceId, String psnNo, byte[] featureVector, String imageUrl) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
        
        FaceFeature faceFeature = new FaceFeature();
        faceFeature.setFaceId(faceId);
        faceFeature.setPsnNo(psnNo);
        faceFeature.setFeatureVector(featureVector);
        faceFeature.setImageUrl(imageUrl);

        faceFeatureMap.put(faceId, faceFeature);
        log.debug("已添加人脸特征: faceId={}, psnNo={}", faceId, psnNo);
        
        return faceId;
    }

    public boolean removeFaceFeature(String faceId) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
        FaceFeature removed = faceFeatureMap.remove(faceId);
        
        if (removed != null) {
            log.info("已删除人脸特征: faceId={}", faceId);
            return true;
        }
        return false;
    }

    public int removeFaceFeaturesBypsnNo(Long psnNo) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
        
        List<String> toRemove = faceFeatureMap.values().stream()
            .filter(feature -> psnNo.equals(feature.getPsnNo()))
            .map(FaceFeature::getFaceId)
            .collect(Collectors.toList());
        
        toRemove.forEach(faceFeatureMap::remove);
        log.info("已删除 {} 条人脸特征，psnNo={}", toRemove.size(), psnNo);
        
        return toRemove.size();
    }

    public List<FaceMatchResult> recognizeFace(FaceRecognitionDTO para) {
        long startTime = System.currentTimeMillis();
        try {
            IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
            
            List<FaceMatchResult> results = faceFeatureMap.aggregate(
                new TopNFaceAggregator(para.getFeatureVector(), matchThreshold, topN));
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("人脸识别完成，耗时: {} ms，匹配结果数: {}", duration, results.size());
            
            return results;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("人脸识别失败，耗时: {} ms", duration, e);
            throw e;
        }
    }

    private double calculateCosineSimilarity(byte[] vector1, byte[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

}
