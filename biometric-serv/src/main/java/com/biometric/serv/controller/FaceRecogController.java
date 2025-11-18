package com.biometric.serv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.biometric.algo.dto.RecogResult;
import com.biometric.algo.service.FaceRecogService;
import com.biometric.serv.entity.FaceFtur;
import com.biometric.serv.mapper.FaceFturMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private FaceRecogService faceSearchService;

    @Autowired
    private FaceFturMapper faceFturDMapper;

    @PostMapping("/compareMore")
    public ResponseEntity<?> compareMore(@RequestParam(required = true)  String personId,
                                         @RequestParam(required = false) String groupIds) {

        FaceFtur bean = faceFturDMapper.selectOne(new QueryWrapper<FaceFtur>()
                .eq("PSN_TMPL_NO", personId).last("limit 1"));

        if (bean == null || bean.getFaceFturData() == null || bean.getFaceFturData().length != 512) {
            log.error("Invalid query feature data");
            return ResponseEntity.badRequest().body("Invalid query feature data");
        }

        Set<String> setGroupIds = new HashSet<>();
        if(groupIds!= null && !groupIds.isEmpty()) {
            for (String groupId : groupIds.split(",")) {
                setGroupIds.add(groupId);
            }
        }

        List<RecogResult> resultList = faceSearchService.searchInGroups(bean.getFaceFturData(), setGroupIds, 0.6f, 3);

        return ResponseEntity.ok(resultList);

    }

}