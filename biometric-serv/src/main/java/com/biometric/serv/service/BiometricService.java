package com.biometric.serv.service;

import com.biometric.algo.dto.FaceRecognitionDTO;
import com.biometric.algo.model.FaceMatchResult;
import com.biometric.algo.service.FaceRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 生物识别业务服务
 */
@Slf4j
@Service
public class BiometricService {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    /**
     * 1:N人脸识别
     */
    public Map<String, Object>  recognizeFace(FaceRecognitionDTO dto) {
        long startTime = System.currentTimeMillis();
        
        try {

            // 调用算法服务进行识别
            List<FaceMatchResult> matchResults = faceRecognitionService.recognizeFace(dto);
            
            long costTime = System.currentTimeMillis() - startTime;
            
            // 转换结果为 Map
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
            log.error("Face recognition failed", e);
            throw new RuntimeException("人脸识别失败: " + e.getMessage());
        }
    }

}

