package com.biometric.algo.service;

import com.biometric.algo.model.FaceFeature;
import com.biometric.algo.model.FaceMatchResult;
import com.biometric.algo.util.Face303JavaCalcuater;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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
    private HazelcastInstance hazelcastInstance;

    @Value("${face.recognition.threshold:0.6}")
    private double matchThreshold;

    @Value("${face.recognition.topN:10}")
    private int topN;

    private static final String FACE_FEATURE_MAP = "faceFeatureMap";

    /**
     * 添加人脸特征到分布式缓存（自动生成 faceId）
     */
    public String addFaceFeature(Long userId, byte[] featureVector, String imageUrl) {
        String faceId = UUID.randomUUID().toString();
        return addFaceFeatureWithId(faceId, userId, featureVector, imageUrl);
    }

    /**
     * 添加人脸特征到分布式缓存（使用指定的 faceId）
     */
    public String addFaceFeatureWithId(String faceId, Long userId, byte[] featureVector, String imageUrl) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
        
        FaceFeature faceFeature = new FaceFeature();
        faceFeature.setFaceId(faceId);
        faceFeature.setUserId(userId);
        faceFeature.setFeatureVector(featureVector);
        faceFeature.setImageUrl(imageUrl);
        faceFeature.setCreateTime(System.currentTimeMillis());
        
        faceFeatureMap.put(faceId, faceFeature);
        log.debug("Added face feature: faceId={}, userId={}", faceId, userId);
        
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
    public int removeFaceFeaturesByUserId(Long userId) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
        
        List<String> toRemove = faceFeatureMap.values().stream()
            .filter(feature -> userId.equals(feature.getUserId()))
            .map(FaceFeature::getFaceId)
            .collect(Collectors.toList());
        
        toRemove.forEach(faceFeatureMap::remove);
        log.info("Removed {} face features for userId={}", toRemove.size(), userId);
        
        return toRemove.size();
    }

    /**
     * 1:N人脸识别 - 在所有特征中搜索最匹配的
     */
    public List<FaceMatchResult> recognizeFace(String queryFeatureVector) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);

        int featureCount = faceFeatureMap.size();
        System.out.println("Total face features: " + featureCount);
        
        List<FaceMatchResult> results = new ArrayList<>();

        byte[] featureVector = Base64.getDecoder().decode(queryFeatureVector);
        int[] bFeat1 =  Face303JavaCalcuater.getBinaFeat(featureVector);

        int HAM_DIST = 283;

        // 遍历所有人脸特征进行比对
        for (FaceFeature feature : faceFeatureMap.values()) {
            double similarity = calculateCosineSimilarity(featureVector, feature.getFeatureVector());

            int[] bFeat2 = Face303JavaCalcuater.getBinaFeat(feature.getFeatureVector());
            boolean isSimilar = Face303JavaCalcuater.isBinaFeatSimilar(
                        bFeat1[0], bFeat1[1], bFeat1[2], bFeat1[3],
                        bFeat2[0], bFeat2[1], bFeat2[2], bFeat2[3], HAM_DIST
                    );
            float similarity2 = 0.0f;
            if (isSimilar){
                similarity2 = Face303JavaCalcuater.compare(Face303JavaCalcuater.toFloatArray(featureVector), Face303JavaCalcuater.toFloatArray(feature.getFeatureVector()));
            }

            if (similarity2 >= matchThreshold) {
                FaceMatchResult result = new FaceMatchResult();
                result.setFaceId(feature.getFaceId());
                result.setUserId(feature.getUserId());
                result.setSimilarity(similarity2+0.0);
                result.setImageUrl(feature.getImageUrl());
                result.setMatched(true);
                results.add(result);
            }
        }
        
        // 按相似度降序排序，取前N个
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        
        if (results.size() > topN) {
            results = results.subList(0, topN);
        }
        
        return results;
    }

    /**
     * 1:1人脸验证 - 验证指定用户
     */
    public FaceMatchResult verifyFace(Long userId, byte[] queryFeatureVector) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
        
        // 获取该用户的所有人脸特征
        List<FaceFeature> userFeatures = faceFeatureMap.values().stream()
            .filter(feature -> userId.equals(feature.getUserId()))
            .collect(Collectors.toList());
        
        if (userFeatures.isEmpty()) {
            log.warn("No face features found for userId={}", userId);
            return null;
        }
        
        // 找到最高相似度
        FaceMatchResult bestMatch = null;
        double maxSimilarity = 0.0;
        
        for (FaceFeature feature : userFeatures) {
            double similarity = calculateCosineSimilarity(queryFeatureVector, feature.getFeatureVector());
            
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestMatch = new FaceMatchResult();
                bestMatch.setFaceId(feature.getFaceId());
                bestMatch.setUserId(feature.getUserId());
                bestMatch.setSimilarity(similarity);
                bestMatch.setImageUrl(feature.getImageUrl());
                bestMatch.setMatched(similarity >= matchThreshold);
            }
        }
        
        log.info("Face verification completed: userId={}, similarity={}, matched={}", 
                userId, maxSimilarity, bestMatch != null && bestMatch.getMatched());
        
        return bestMatch;
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

    /**
     * 获取人脸特征总数
     */
    public int getFaceFeatureCount() {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
        return faceFeatureMap.size();
    }

    /**
     * 获取指定用户的人脸特征列表
     */
    public List<FaceFeature> getFaceFeaturesByUserId(Long userId) {
        IMap<String, FaceFeature> faceFeatureMap = hazelcastInstance.getMap(FACE_FEATURE_MAP);
        
        return faceFeatureMap.values().stream()
            .filter(feature -> userId.equals(feature.getUserId()))
            .collect(Collectors.toList());
    }
}

