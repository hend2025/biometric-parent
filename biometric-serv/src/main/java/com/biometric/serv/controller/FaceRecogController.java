package com.biometric.serv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/faceRecog")
public class FaceRecogController {

    private static final Logger log = LoggerFactory.getLogger(FaceRecogController.class);
    private static final int MAX_GROUP_IDS_COUNT = 50;

    @Value("${biometric.recognition.threshold:0.6}")
    private double recognitionThreshold;
    
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

        personId = personId.trim();

        QueryWrapper<FaceFtur> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("PSN_TMPL_NO", personId);
        List<FaceFtur> features = faceFturDMapper.selectList(queryWrapper);
        
        if (features == null || features.isEmpty()) {
            log.warn("No feature found for personId: {}", personId);
            return ResponseEntity.badRequest().body("No feature found for personId: " + personId);
        }
        
        FaceFtur bean = features.get(0);

        if (bean.getFaceFturData() == null || bean.getFaceFturData().length != 512) {
            log.error("Invalid query feature data for personId: {}", personId);
            return ResponseEntity.badRequest().body("Invalid query feature data");
        }

        Set<String> setGroupIds = new HashSet<>();
        if (groupIds != null && !groupIds.trim().isEmpty()) {
            String[] groupIdArray = groupIds.split(",");

            for (String groupId : groupIdArray) {
                String trimmedGroupId = groupId.trim();
                if (!trimmedGroupId.isEmpty()) {
                    setGroupIds.add(trimmedGroupId);
                }
            }
        }

        List<RecogResult> resultList;
        try {
            resultList = faceSearchService.searchInGroups(bean.getFaceFturData(), setGroupIds, recognitionThreshold, topN);
        } catch (Exception e) {
            log.error("Error during face recognition for personId: " + personId, e);
            return ResponseEntity.status(500).body("Face recognition failed: " + e.getMessage());
        }

        log.info("Face recognition completed for personId: {}, found {} matches", personId, resultList.size());
        return ResponseEntity.ok(resultList);
    }

}