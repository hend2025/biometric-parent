package com.biometric.serv.controller;

import com.biometric.serv.service.FaceFeatureLoadService;
import com.biometric.serv.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/face/load")
public class FaceFeatureLoadController {

    @Autowired
    private FaceFeatureLoadService faceFeatureLoadService;

    @PostMapping("/all")
    public ResultVO<Map<String, Object>> loadAllFaceFeatures() {
        log.info("接收到手动加载人脸特征数据请求");
        
        try {
            long startTime = System.currentTimeMillis();
            long totalCount = faceFeatureLoadService.getTotalFaceFeatureCount();
            
            Map<String, Object> data = new HashMap<>();
            data.put("totalCount", totalCount);
            
            if (totalCount > 0) {
                new Thread(() -> {
                    faceFeatureLoadService.loadFaceFeaturesToHazelcast();
                }).start();
                
                long endTime = System.currentTimeMillis();
                data.put("message", "人脸特征数据加载已启动，请稍后查询加载进度");
                data.put("costTime", endTime - startTime);
                
                return ResultVO.success(data);
            } else {
                data.put("message", "数据库中没有有效的人脸特征数据需要加载");
                return ResultVO.success(data);
            }
            
        } catch (Exception e) {
            log.error("手动加载人脸特征数据失败", e);
            return ResultVO.error("加载失败: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    public ResultVO<Map<String, Object>> getFaceFeatureCount() {
        try {
            long totalCount = faceFeatureLoadService.getTotalFaceFeatureCount();
            
            Map<String, Object> data = new HashMap<>();
            data.put("totalCount", totalCount);
            
            return ResultVO.success(data);
            
        } catch (Exception e) {
            log.error("查询人脸特征数据总数失败", e);
            return ResultVO.error("查询失败: " + e.getMessage());
        }
    }

}
