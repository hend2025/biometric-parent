package com.biometric.serv.service;

import com.alibaba.fastjson.JSON;
import com.biometric.serv.dto.FaceRecognitionDTO;
import com.biometric.serv.dto.FaceRegisterDTO;
import com.biometric.serv.dto.FaceVerificationDTO;
import com.biometric.serv.entity.FaceRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生物识别业务服务
 */
@Slf4j
@Service
public class BiometricService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private FaceRecordService faceRecordService;

    @Autowired
    private RecognitionLogService recognitionLogService;

    /**
     * 注册人脸
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> registerFace(FaceRegisterDTO dto) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 调用算法服务添加人脸特征
            Map<String, Object> request = new HashMap<>();
            request.put("userId", dto.getUserId());
            request.put("featureVector", dto.getFeatureVector());
            request.put("imageUrl", dto.getImageUrl());
            
            Map<String, Object> algoResponse = webClient.post()
                    .uri("/api/algo/face/feature")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (algoResponse != null && Boolean.TRUE.equals(algoResponse.get("success"))) {
                String faceId = (String) algoResponse.get("faceId");
                
                // 保存人脸记录到数据库
                FaceRecord record = faceRecordService.createFaceRecord(
                    faceId, dto.getUserId(), dto.getImageUrl(), 
                    dto.getFeatureVector(), dto.getRemark()
                );
                
                Map<String, Object> result = new HashMap<>();
                result.put("faceId", faceId);
                result.put("recordId", record.getId());
                result.put("costTime", System.currentTimeMillis() - startTime);
                
                log.info("Face registered successfully: faceId={}, userId={}", faceId, dto.getUserId());
                return result;
            } else {
                throw new RuntimeException("算法服务调用失败");
            }
        } catch (Exception e) {
            log.error("Face registration failed", e);
            throw new RuntimeException("人脸注册失败: " + e.getMessage());
        }
    }

    /**
     * 删除人脸
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFace(String faceId) {
        try {
            // 调用算法服务删除人脸特征
            Map<String, Object> algoResponse = webClient.delete()
                    .uri("/api/algo/face/feature/" + faceId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            // 删除数据库记录
            faceRecordService.deleteFaceRecord(faceId);
            
            log.info("Face deleted successfully: faceId={}", faceId);
            return true;
        } catch (Exception e) {
            log.error("Face deletion failed", e);
            throw new RuntimeException("人脸删除失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID删除所有人脸
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteFacesByUserId(Long userId) {
        try {
            // 调用算法服务删除用户所有人脸特征
            Map<String, Object> algoResponse = webClient.delete()
                    .uri("/api/algo/face/feature/user/" + userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            // 删除数据库记录
            int count = faceRecordService.deleteFaceRecordsByUserId(userId);
            
            log.info("Deleted {} faces for userId={}", count, userId);
            return count;
        } catch (Exception e) {
            log.error("Face deletion by userId failed", e);
            throw new RuntimeException("人脸删除失败: " + e.getMessage());
        }
    }

    /**
     * 1:N人脸识别
     */
    public Map<String, Object> recognizeFace(FaceRecognitionDTO dto) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 调用算法服务进行识别
            Map<String, Object> request = new HashMap<>();
            request.put("featureVector", dto.getFeatureVector());
            
            Map<String, Object> algoResponse = webClient.post()
                    .uri("/api/algo/face/recognize")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            long costTime = System.currentTimeMillis() - startTime;
            
            if (algoResponse != null && Boolean.TRUE.equals(algoResponse.get("success"))) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) algoResponse.get("results");
                
                // 记录日志
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> topResult = results.get(0);
                    Long userId = getLongValue(topResult.get("userId"));
                    String faceId = (String) topResult.get("faceId");
                    Double similarity = getDoubleValue(topResult.get("similarity"));
                    Boolean matched = (Boolean) topResult.get("matched");
                    
                    recognitionLogService.createLog(
                        userId, faceId, similarity, 1, 
                        Boolean.TRUE.equals(matched) ? 1 : 0,
                        dto.getQueryImageUrl(), costTime
                    );
                } else {
                    recognitionLogService.createLog(
                        null, null, null, 1, 0,
                        dto.getQueryImageUrl(), costTime
                    );
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("results", results);
                result.put("count", results != null ? results.size() : 0);
                result.put("costTime", costTime);
                
                return result;
            } else {
                throw new RuntimeException("算法服务调用失败");
            }
        } catch (Exception e) {
            log.error("Face recognition failed", e);
            throw new RuntimeException("人脸识别失败: " + e.getMessage());
        }
    }

    /**
     * 1:1人脸验证
     */
    public Map<String, Object> verifyFace(FaceVerificationDTO dto) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 调用算法服务进行验证
            Map<String, Object> request = new HashMap<>();
            request.put("userId", dto.getUserId());
            request.put("featureVector", dto.getFeatureVector());
            
            Map<String, Object> algoResponse = webClient.post()
                    .uri("/api/algo/face/verify")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            long costTime = System.currentTimeMillis() - startTime;
            
            if (algoResponse != null && Boolean.TRUE.equals(algoResponse.get("success"))) {
                Map<String, Object> result = (Map<String, Object>) algoResponse.get("result");
                
                // 记录日志
                if (result != null) {
                    String faceId = (String) result.get("faceId");
                    Double similarity = getDoubleValue(result.get("similarity"));
                    Boolean matched = (Boolean) result.get("matched");
                    
                    recognitionLogService.createLog(
                        dto.getUserId(), faceId, similarity, 2,
                        Boolean.TRUE.equals(matched) ? 1 : 0,
                        dto.getQueryImageUrl(), costTime
                    );
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("result", result);
                    response.put("costTime", costTime);
                    return response;
                } else {
                    recognitionLogService.createLog(
                        dto.getUserId(), null, null, 2, 0,
                        dto.getQueryImageUrl(), costTime
                    );
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("result", null);
                    response.put("message", "用户未注册人脸");
                    response.put("costTime", costTime);
                    return response;
                }
            } else {
                throw new RuntimeException("算法服务调用失败");
            }
        } catch (Exception e) {
            log.error("Face verification failed", e);
            throw new RuntimeException("人脸验证失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的人脸列表
     */
    public List<FaceRecord> getUserFaces(Long userId) {
        return faceRecordService.listFaceRecordsByUserId(userId);
    }

    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        return Long.valueOf(value.toString());
    }

    private Double getDoubleValue(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Float) return ((Float) value).doubleValue();
        return Double.valueOf(value.toString());
    }
}

