package com.biometric.serv.controller;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.dto.CompareParams;
import com.biometric.algo.dto.CompareResult;
import com.biometric.algo.dto.SocketFaceFeature;
import com.biometric.algo.service.FaceAlgoService;
import com.biometric.algo.service.FaceRecogService;
import com.biometric.algo.util.ImageToBase64Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/faceRecog")
public class FaceRecogController {

    private static final Logger log = LoggerFactory.getLogger(FaceRecogController.class);

    @Value("${biometric.recognition.threshold:0.6}")
    private float threshold;
    
    @Value("${biometric.recognition.top-n:3}")
    private int topN;

    @Autowired
    private FaceRecogService faceSearchService;

    @Autowired
    private FaceAlgoService faceAlgoService;

    @PostMapping("/compareMore")
    public ResponseEntity<?> compareMore(@RequestParam(required = true) String personId,
                                         @RequestParam(required = false) String groupIds) throws IOException {
        if (personId == null || personId.trim().isEmpty()) {
            log.error("PersonId is null or empty");
            return ResponseEntity.badRequest().body("PersonId is required");
        }

        String filePath = "D:\\pic\\"+personId+".jpg";
        File file = new File(filePath);
        if(!file.exists()){
            return ResponseEntity.badRequest().body("PersonId not exists");
        }

        String imageBase64 = ImageToBase64Util.convertImageToBase64(filePath);
        JSONObject images = new JSONObject();
        images.put("0", imageBase64);

        SocketFaceFeature featureResult = faceAlgoService.faceExtractFeature(images);
        if (featureResult.getReturnId() != 0 || featureResult.getReturnValue() == null) {
            return ResponseEntity.badRequest().body("提取特征失败: " + personId);
        }

        List<byte[]> features = new ArrayList<>();
        String extractedFeature = featureResult.getReturnValue().getFeature().getFeatureValue().getString("0");
        byte[] bytes = Base64.getDecoder().decode(extractedFeature);
        features.add(bytes);
        features.add(bytes);

        List<String> setGroupIds = new ArrayList<>();
        if (groupIds != null && !groupIds.trim().isEmpty()) {
            setGroupIds = Arrays.asList(groupIds.split(","));
        }

        CompareParams recogParam = new CompareParams();
        recogParam.setFeatures(features);
        recogParam.setGroups(setGroupIds);
        recogParam.setThreshold(threshold);
        recogParam.setTopN(topN);

        List<CompareResult> resultList = faceSearchService.recogOneToMany(recogParam);

        log.info("Face recognition completed for personId: {}, found {} matches", personId, resultList.size());
        return ResponseEntity.ok(resultList);
    }

}