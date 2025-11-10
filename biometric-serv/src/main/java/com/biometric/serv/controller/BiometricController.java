package com.biometric.serv.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biometric.serv.dto.FaceRecognitionDTO;
import com.biometric.serv.dto.FaceRegisterDTO;
import com.biometric.serv.dto.FaceVerificationDTO;
import com.biometric.serv.entity.FaceRecord;
import com.biometric.serv.entity.RecognitionLog;
import com.biometric.serv.service.BiometricService;
import com.biometric.serv.service.RecognitionLogService;
import com.biometric.serv.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 生物识别业务控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/biometric")
public class BiometricController {

    @Autowired
    private BiometricService biometricService;

    @Autowired
    private RecognitionLogService recognitionLogService;

    /**
     * 注册人脸
     */
    @PostMapping("/face/register")
    public ResultVO<Map<String, Object>> registerFace(@Validated @RequestBody FaceRegisterDTO dto) {
        try {
            Map<String, Object> result = biometricService.registerFace(dto);
            return ResultVO.success("人脸注册成功", result);
        } catch (Exception e) {
            log.error("Register face failed", e);
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 删除人脸
     */
    @DeleteMapping("/face/{faceId}")
    public ResultVO<Boolean> deleteFace(@PathVariable String faceId) {
        try {
            boolean success = biometricService.deleteFace(faceId);
            return ResultVO.success("人脸删除成功", success);
        } catch (Exception e) {
            log.error("Delete face failed", e);
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 根据用户ID删除所有人脸
     */
    @DeleteMapping("/face/user/{userId}")
    public ResultVO<Integer> deleteFacesByUserId(@PathVariable Long userId) {
        try {
            int count = biometricService.deleteFacesByUserId(userId);
            return ResultVO.success("删除成功，共删除" + count + "个人脸", count);
        } catch (Exception e) {
            log.error("Delete faces by userId failed", e);
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 1:N人脸识别
     */
    @PostMapping("/face/recognize")
    public ResultVO<Map<String, Object>> recognizeFace(@Validated @RequestBody FaceRecognitionDTO dto) {
        try {
            Map<String, Object> result = biometricService.recognizeFace(dto);
            return ResultVO.success("识别完成", result);
        } catch (Exception e) {
            log.error("Face recognition failed", e);
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 1:1人脸验证
     */
    @PostMapping("/face/verify")
    public ResultVO<Map<String, Object>> verifyFace(@Validated @RequestBody FaceVerificationDTO dto) {
        try {
            Map<String, Object> result = biometricService.verifyFace(dto);
            return ResultVO.success("验证完成", result);
        } catch (Exception e) {
            log.error("Face verification failed", e);
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 获取用户的人脸列表
     */
    @GetMapping("/face/user/{userId}")
    public ResultVO<List<FaceRecord>> getUserFaces(@PathVariable Long userId) {
        try {
            List<FaceRecord> faces = biometricService.getUserFaces(userId);
            return ResultVO.success(faces);
        } catch (Exception e) {
            log.error("Get user faces failed", e);
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 分页查询识别日志
     */
    @GetMapping("/log/list")
    public ResultVO<Page<RecognitionLog>> listRecognitionLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer recognitionType) {
        try {
            Page<RecognitionLog> page = recognitionLogService.listLogs(pageNum, pageSize, userId, recognitionType);
            return ResultVO.success(page);
        } catch (Exception e) {
            log.error("List recognition logs failed", e);
            return ResultVO.error(e.getMessage());
        }
    }
}

