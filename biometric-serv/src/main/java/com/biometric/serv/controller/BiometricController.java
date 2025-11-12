package com.biometric.serv.controller;

import com.biometric.algo.dto.FaceRecognitionDTO;
import com.biometric.serv.entity.BosgFaceFturD;
import com.biometric.serv.mapper.BosgFaceFturDMapper;
import com.biometric.serv.service.BiometricService;
import com.biometric.serv.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/biometric")
public class BiometricController {

    @Autowired
    private BiometricService biometricService;

    @Autowired
    private BosgFaceFturDMapper bosgFaceFturDMapper;

    @PostMapping("/face/recognize")
    public ResultVO<Map<String, Object>> recognizeFace() {
        try {
            BosgFaceFturD bean = bosgFaceFturDMapper.selectById("345854003557139474");
            FaceRecognitionDTO dto = new FaceRecognitionDTO();
            dto.setFeatureVector(bean.getFaceFturData());

            Map<String, Object> result = biometricService.recognizeFace(dto);
            return ResultVO.success("识别完成", result);
        } catch (Exception e) {
            log.error("人脸识别失败", e);
            return ResultVO.error(e.getMessage());
        }
    }

}
