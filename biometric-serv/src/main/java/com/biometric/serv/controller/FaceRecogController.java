package com.biometric.serv.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.biometric.algo.dto.RecogParam;
import com.biometric.algo.dto.RecogResult;
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

    @PostMapping("/compareMore")
    public ResponseEntity<?> compareMore(@RequestParam(required = true) String personId,
                                         @RequestParam(required = false) String groupIds) {

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

        RecogParam recogParam = new RecogParam();
        recogParam.setFeatures(features);
        recogParam.setGroups(setGroupIds);
        recogParam.setThreshold(threshold);
        recogParam.setTopN(topN);

        List<RecogResult> resultList = faceSearchService.recogOneToMany(recogParam);

        log.info("Face recognition completed for personId: {}, found {} matches", personId, resultList.size());
        return ResponseEntity.ok(resultList);
    }

}