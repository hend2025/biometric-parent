package com.biometric.algo.controller;

import com.biometric.algo.model.FaceFeature;
import com.biometric.algo.model.FaceMatchResult;
import com.biometric.algo.service.FaceRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 人脸识别算法接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/algo/face")
public class FaceRecognitionController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    /**
     * 添加人脸特征
     */
    @PostMapping("/feature")
    public ResponseEntity<Map<String, Object>> addFaceFeature(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String imageUrl = (String) request.get("imageUrl");
        
        // 这里假设前端传入的是特征向量数组
        List<Number> featureList = (List<Number>) request.get("featureVector");
        float[] featureVector = new float[featureList.size()];
        for (int i = 0; i < featureList.size(); i++) {
            featureVector[i] = featureList.get(i).floatValue();
        }
        
        String faceId = faceRecognitionService.addFaceFeature(userId, featureVector, imageUrl);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("faceId", faceId);
        response.put("message", "Face feature added successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 删除人脸特征
     */
    @DeleteMapping("/feature/{faceId}")
    public ResponseEntity<Map<String, Object>> removeFaceFeature(@PathVariable String faceId) {
        boolean success = faceRecognitionService.removeFaceFeature(faceId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Face feature removed successfully" : "Face feature not found");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据用户ID删除所有人脸特征
     */
    @DeleteMapping("/feature/user/{userId}")
    public ResponseEntity<Map<String, Object>> removeFaceFeaturesByUserId(@PathVariable Long userId) {
        int count = faceRecognitionService.removeFaceFeaturesByUserId(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", count);
        response.put("message", count + " face features removed");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 1:N人脸识别
     */
    @PostMapping("/recognize")
    public ResponseEntity<Map<String, Object>> recognizeFace(@RequestBody Map<String, Object> request) {
        List<Number> featureList = (List<Number>) request.get("featureVector");
        float[] featureVector = new float[featureList.size()];
        for (int i = 0; i < featureList.size(); i++) {
            featureVector[i] = featureList.get(i).floatValue();
        }
        
        List<FaceMatchResult> results = faceRecognitionService.recognizeFace(featureVector);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("results", results);
        response.put("count", results.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 1:1人脸验证
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyFace(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        
        List<Number> featureList = (List<Number>) request.get("featureVector");
        float[] featureVector = new float[featureList.size()];
        for (int i = 0; i < featureList.size(); i++) {
            featureVector[i] = featureList.get(i).floatValue();
        }
        
        FaceMatchResult result = faceRecognitionService.verifyFace(userId, featureVector);
        
        Map<String, Object> response = new HashMap<>();
        if (result != null) {
            response.put("success", true);
            response.put("result", result);
        } else {
            response.put("success", false);
            response.put("message", "No face features found for user");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取人脸特征总数
     */
    @GetMapping("/feature/count")
    public ResponseEntity<Map<String, Object>> getFaceFeatureCount() {
        int count = faceRecognitionService.getFaceFeatureCount();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", count);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取指定用户的人脸特征列表
     */
    @GetMapping("/feature/user/{userId}")
    public ResponseEntity<Map<String, Object>> getFaceFeaturesByUserId(@PathVariable Long userId) {
        List<FaceFeature> features = faceRecognitionService.getFaceFeaturesByUserId(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("features", features);
        response.put("count", features.size());
        
        return ResponseEntity.ok(response);
    }
}

