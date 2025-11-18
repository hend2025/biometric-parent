package com.biometric.serv.controller;

import com.biometric.algo.dto.RecogResult;
import com.biometric.algo.service.FaceRecogService;
import com.biometric.serv.entity.FaceFtur;
import com.biometric.serv.mapper.FaceFturMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.hazelcast.internal.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<?> compareMore() {

        FaceFtur bean = faceFturDMapper.selectById("2");

        if (bean == null || bean.getFaceFturData() == null || bean.getFaceFturData().length != 512) {
            log.error("Invalid query feature data");
            return ResponseEntity.badRequest().body("Invalid query feature data");
        }

        List<RecogResult> resultList1 = faceSearchService.searchInAll(bean.getFaceFturData(), 0.6f, 3);
        System.out.println(resultList1);

        Set<String> groupIds = new HashSet<>();
        groupIds.add("344016480595477581");

        List<RecogResult>  resultList2 = faceSearchService.searchInGroups(bean.getFaceFturData(), groupIds,0.6f, 3);

        return ResponseEntity.ok(resultList2);

    }

}