package com.biometric.serv.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biometric.algo.dto.CompareParams;
import com.biometric.algo.dto.CompareResult;
import com.biometric.algo.service.FaceRecogService;
import com.biometric.serv.entity.FaceFtur;
import com.biometric.serv.mapper.FaceFturMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/v1/faceRecog")
public class FaceRecogController {

    private static final Logger log = LoggerFactory.getLogger(FaceRecogController.class);

    @Value("${biometric.recognition.threshold:0.6}")
    private float threshold;
    
    @Value("${biometric.recognition.top-n:3}")
    private int topN;

    @Autowired
    private FaceRecogService faceSearchService;

    @Autowired
    private FaceFturMapper faceFturDMapper;

    @Autowired
    SocketServiceGemini socketService;

    @PostMapping("/compareMore")
    public ResponseEntity<?> compareMore(@RequestParam(required = true) String personId,
                                         @RequestParam(required = false) String groupIds) throws IOException {

//        String imagePath = "D:\\Users\\hend\\Desktop\\model\\zxc.jpg";
//        String imageBase64 = ImageToBase64Util.convertImageToBase64(imagePath);
//
//        JSONObject images = new JSONObject();
//        images.put("0", imageBase64);
//
//        SocketResponse<FaceDataResponse> response  = socketService.faceExtractFeature(images,  true, false);
//
//        FaceDataResponse v =  response.getReturnValue() ;
//        FaceDataResponse.FeatureInfo feature1 = v.getFeature();
//        System.out.println(feature1.getFeatureData());
//
//        if (response.getReturnId() == 0) {
////            // 2. 第二步：解析内部的 RETURNVALUE 字符串
////            String innerJson = response.getReturnValue();
////            FaceDataResponse faceData = JSON.parseObject(innerJson, FaceDataResponse.class);
////
////            // 访问数据
////            System.out.println("Min Score: " + faceData.getMinScore());
////
////            // 访问 Attachment (注意 Key 是 "0")
////            if (faceData.getAttachment() != null && faceData.getAttachment().containsKey("0")) {
////                FaceDataResponse.Attachment att = faceData.getAttachment().get("0");
////                System.out.println("Pic Width: " + att.getPicWidth());
////            }
////
////            // 注意：FaceData 中的 feature 字段本身也是个 String，如果需要用到特征值，可能需要第三次解析
////            System.out.println("Feature String: " + faceData.getFeature().getFeatureData());
//        }
//
//
//        System.out.println(response.toString());

        if (personId == null || personId.trim().isEmpty()) {
            log.error("PersonId is null or empty");
            return ResponseEntity.badRequest().body("PersonId is required");
        }

        List<FaceFtur> faceFturList = faceFturDMapper.selectList(
                new LambdaQueryWrapper<FaceFtur>().eq(FaceFtur::getPsnTmplNo,personId)
        );
        if (faceFturList == null || faceFturList.isEmpty()) {
            log.warn("No feature found for personId: {}", personId);
            return ResponseEntity.badRequest().body("No feature found for personId: " + personId);
        }

        List<byte[]> features = new ArrayList<>();
        for(FaceFtur faceFtur: faceFturList){
            features.add(faceFtur.getFaceFturData());
        }

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