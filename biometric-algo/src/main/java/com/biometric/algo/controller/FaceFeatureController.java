package com.biometric.algo.controller;

import com.biometric.algo.service.FaceRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 人脸特征管理接口控制器
 * 提供用于数据加载的 API 接口
 */
@Slf4j
@RestController
@RequestMapping("/face")
public class FaceFeatureController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    /**
     * 添加人脸特征（用于数据加载）
     * 支持指定 faceId，适用于从数据库加载现有数据
     */
    @PostMapping("/feature/add")
    public ResponseEntity<Map<String, Object>> addFaceFeature(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取参数
            String faceId = (String) request.get("faceId");
            Object userIdObj = request.get("userId");
            String imageUrl = (String) request.get("imageUrl");
            
            // 转换 userId（支持 String 或 Long 类型）
            Long userId;
            if (userIdObj instanceof String) {
                userId = Long.parseLong((String) userIdObj);
            } else if (userIdObj instanceof Number) {
                userId = ((Number) userIdObj).longValue();
            } else {
                response.put("code", -1);
                response.put("message", "Invalid userId parameter");
                return ResponseEntity.badRequest().body(response);
            }

            Object featureVectorObj = request.get("featureVector");
            byte[] featureVector;
            if (featureVectorObj instanceof byte[]) {
                featureVector = (byte[]) featureVectorObj;
            } else if (featureVectorObj instanceof String) {
                try {
                    // 假设传入的是Base64编码的字符串
                    featureVector = Base64.getDecoder().decode((String) featureVectorObj);
                } catch (IllegalArgumentException e) {
                    response.put("code", -1);
                    response.put("message", "Invalid Base64 encoded featureVector");
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                response.put("code", -1);
                response.put("message", "Invalid featureVector parameter type");
                return ResponseEntity.badRequest().body(response);
            }
            if (featureVector == null || featureVector.length == 0) {
                response.put("code", -1);
                response.put("message", "Feature vector is required");
                return ResponseEntity.badRequest().body(response);
            }

            // 添加人脸特征（如果指定了 faceId，则使用指定的 ID）
            String resultFaceId;
            if (faceId != null && !faceId.isEmpty()) {
                // 使用指定的 faceId
                resultFaceId = faceRecognitionService.addFaceFeatureWithId(faceId, userId, featureVector, imageUrl);
            } else {
                // 自动生成 faceId
                resultFaceId = faceRecognitionService.addFaceFeature(userId, featureVector, imageUrl);
            }
            
            response.put("code", 0);
            response.put("message", "Success");
            response.put("data", resultFaceId);
            
            log.debug("Added face feature: faceId={}, userId={}", resultFaceId, userId);
            
        } catch (Exception e) {
            log.error("Failed to add face feature", e);
            response.put("code", -1);
            response.put("message", "Failed to add face feature: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 批量添加人脸特征
     */
    @PostMapping("/feature/batch")
    public ResponseEntity<Map<String, Object>> batchAddFaceFeatures(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> features = (List<Map<String, Object>>) request.get("features");
            
            if (features == null || features.isEmpty()) {
                response.put("code", -1);
                response.put("message", "Features list is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (Map<String, Object> feature : features) {
                try {
                    String faceId = (String) feature.get("faceId");
                    Object userIdObj = feature.get("userId");
                    String imageUrl = (String) feature.get("imageUrl");
                    
                    Long userId;
                    if (userIdObj instanceof String) {
                        userId = Long.parseLong((String) userIdObj);
                    } else {
                        userId = ((Number) userIdObj).longValue();
                    }
                    // 特征向量
                    byte[] featureVector = (byte[]) request.get("featureVector");
                    if (featureVector == null) {
                        response.put("code", -1);
                        response.put("message", "Feature vector is required");
                        return ResponseEntity.badRequest().body(response);
                    }
                    
                    if (faceId != null && !faceId.isEmpty()) {
                        faceRecognitionService.addFaceFeatureWithId(faceId, userId, featureVector, imageUrl);
                    } else {
                        faceRecognitionService.addFaceFeature(userId, featureVector, imageUrl);
                    }
                    
                    successCount++;
                    
                } catch (Exception e) {
                    log.error("Failed to add face feature in batch", e);
                    failCount++;
                }
            }
            
            response.put("code", 0);
            response.put("message", "Batch add completed");
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("total", features.size());
            
            log.info("Batch add face features completed: success={}, fail={}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("Failed to batch add face features", e);
            response.put("code", -1);
            response.put("message", "Failed to batch add face features: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
}

