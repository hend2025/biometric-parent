package com.biometric.algo.service;

import com.biometric.algo.aggregator.TopNFaceAggregator;
import com.biometric.algo.dto.FaceRecognitionDTO;
import com.biometric.algo.model.FaceFeature;
import com.biometric.algo.model.FaceMatchResult;
import com.biometric.algo.util.Face303JavaCalcuater;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 人脸识别服务 - 提供1:N识别能力
 */
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

    /**
     * 添加人脸特征到分布式缓存（自动生成 faceId）
     */
    public String addFaceFeature(String psnNo, byte[] featureVector, String imageUrl) {
        String faceId = UUID.randomUUID().toString();
        return addFaceFeatureWithId(faceId, psnNo, featureVector, imageUrl);
    }

    /**
     * 添加人脸特征到分布式缓存（使用指定的 faceId）
     */
    public String addFaceFeatureWithId(String faceId, String psnNo, byte[] featureVector, String imageUrl) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
        
        FaceFeature faceFeature = new FaceFeature();
        faceFeature.setFaceId(faceId);
        faceFeature.setPsnNo(psnNo);
        faceFeature.setFeatureVector(featureVector);
        faceFeature.setImageUrl(imageUrl);

        faceFeatureMap.put(faceId, faceFeature);
        log.debug("Added face feature: faceId={}, psnNo={}", faceId, psnNo);
        
        return faceId;
    }

    /**
     * 删除人脸特征
     */
    public boolean removeFaceFeature(String faceId) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
        FaceFeature removed = faceFeatureMap.remove(faceId);
        
        if (removed != null) {
            log.info("Removed face feature: faceId={}", faceId);
            return true;
        }
        return false;
    }

    /**
     * 根据用户ID删除所有人脸特征
     */
    public int removeFaceFeaturesBypsnNo(Long psnNo) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
        
        List<String> toRemove = faceFeatureMap.values().stream()
            .filter(feature -> psnNo.equals(feature.getPsnNo()))
            .map(FaceFeature::getFaceId)
            .collect(Collectors.toList());
        
        toRemove.forEach(faceFeatureMap::remove);
        log.info("Removed {} face features for psnNo={}", toRemove.size(), psnNo);
        
        return toRemove.size();
    }

    /**
     * 1:N人脸识别 - 在所有特征中搜索最匹配的
     */
    public List<FaceMatchResult> recognizeFace(FaceRecognitionDTO para) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);

        // 使用聚合器进行人脸匹配，提高性能
        List<FaceMatchResult> results = faceFeatureMap.aggregate(new TopNFaceAggregator(para.getFeatureVector(), matchThreshold, topN));

        return results;
    }

    /**
     * 计算余弦相似度
     */
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

