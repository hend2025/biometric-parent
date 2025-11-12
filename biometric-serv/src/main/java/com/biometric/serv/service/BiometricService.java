package com.biometric.serv.service;

import com.biometric.algo.dto.FaceRecognitionDTO;
import com.biometric.algo.model.FaceMatchResult;
import com.biometric.algo.service.FaceRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
public class BiometricService {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private PerformanceMonitorService performanceMonitorService;

    public Map<String, Object>  recognizeFace(FaceRecognitionDTO dto) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        
        try {

            List<FaceMatchResult> matchResults = faceRecognitionService.recognizeFace(dto);
            
            long costTime = System.currentTimeMillis() - startTime;
            success = true;
            
            List<Map<String, Object>> results = new ArrayList<>();
            for (FaceMatchResult matchResult : matchResults) {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("faceId", matchResult.getFaceId());
                resultMap.put("psnNo", matchResult.getPsnNo());
                resultMap.put("similarity", matchResult.getSimilarity());
                resultMap.put("matched", matchResult.getMatched());
                results.add(resultMap);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("results", results);
            result.put("count", results.size());
            result.put("costTime", costTime);

            return result;
        } catch (Exception e) {
            log.error("人脸识别失败", e);
            throw new RuntimeException("人脸识别失败: " + e.getMessage());
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            performanceMonitorService.recordRecognition(success, duration);
        }
    }

}
