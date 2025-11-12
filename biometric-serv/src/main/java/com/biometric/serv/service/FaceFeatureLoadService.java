package com.biometric.serv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.biometric.algo.service.FaceRecognitionService;
import com.biometric.serv.entity.BosgFaceFturD;
import com.biometric.serv.mapper.BosgFaceFturDMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 人脸特征加载服务
 * 用于在服务启动时从数据库加载人脸特征数据到 Hazelcast
 */
@Slf4j
@Service
public class FaceFeatureLoadService {

    @Autowired
    private BosgFaceFturDMapper bosgFaceFturDMapper;

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    /**
     * 加载人脸特征数据到 Hazelcast
     */
    public void loadFaceFeaturesToHazelcast() {
        log.info("========== 开始加载人脸特征数据到 Hazelcast ==========");
        
        try {
            // 查询有效的人脸特征数据
            QueryWrapper<BosgFaceFturD> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("VALI_FLAG", "1")  // 有效标志
                       .eq("FACE_TMPL_STAS", "1")  // 人脸模板状态为有效
                       .isNotNull("FACE_FTUR_DATA");  // 人脸特征数据不为空
            
            List<BosgFaceFturD> faceFeatures = bosgFaceFturDMapper.selectList(queryWrapper);
            
            if (faceFeatures == null || faceFeatures.isEmpty()) {
                log.warn("没有找到需要加载的人脸特征数据");
                return;
            }
            
            log.info("查询到 {} 条人脸特征数据，开始加载到 Hazelcast", faceFeatures.size());
            
            int successCount = 0;
            int failCount = 0;
            List<String> list = new ArrayList<>();
            for (BosgFaceFturD faceFeature : faceFeatures) {
                try {
                    byte[] featureVector = faceFeature.getFaceFturData();
                    int len = featureVector.length;
                    if (featureVector == null || len != 512) {
                        list.add(faceFeature.getFaceBosgId());
                        log.warn("人脸特征数据转换失败，跳过: faceBosgId={}", faceFeature.getFaceBosgId());
                        failCount++;
                        continue;
                    }
                    
                    // 调用算法服务添加人脸特征
                    boolean success = addFaceFeatureToAlgoService(
                        faceFeature.getFaceBosgId(),
                        faceFeature.getPsnTmplNo(),
                        featureVector,
                        faceFeature.getFaceImgUrl()
                    );
                    
                    if (success) {
                        successCount++;
                        if (successCount % 100 == 0) {
                            log.info("已成功加载 {} 条人脸特征数据", successCount);
                        }
                    } else {
                        failCount++;
                        log.warn("加载人脸特征失败: faceBosgId={}", faceFeature.getFaceBosgId());
                    }
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("处理人脸特征数据异常: faceBosgId={}, error={}", 
                            faceFeature.getFaceBosgId(), e.getMessage());
                }
            }
            if (!list.isEmpty()) {
                log.warn("人脸特征数据长度不是 512 字节: {}", list);
            }
            log.info("========== 人脸特征数据加载完成 ==========");
            log.info("总数: {}, 成功: {}, 失败: {}", faceFeatures.size(), successCount, failCount);
            
        } catch (Exception e) {
            log.error("加载人脸特征数据到 Hazelcast 失败", e);
        }
    }

    /**
     * 将 byte[] 转换为 float[]
     * 人脸特征数据通常是 128 维或 512 维的浮点数数组
     */
    private float[] bytesToFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        // 4 bytes per float
        if (bytes.length % 4 != 0) {
            log.warn("人脸特征数据长度不是 4 的倍数: {}", bytes.length);
            return null;
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);  // 根据实际情况调整字节序
        
        float[] floatArray = new float[bytes.length / 4];
        for (int i = 0; i < floatArray.length; i++) {
            floatArray[i] = buffer.getFloat();
        }
        
        return floatArray;
    }

    /**
     * 添加人脸特征到 Hazelcast
     */
    private boolean addFaceFeatureToAlgoService(String faceId, String psnNo,
                                                byte[] featureVector, String imageUrl) {
        try {
            faceRecognitionService.addFaceFeatureWithId(faceId, psnNo, featureVector, imageUrl);
            return true;
        } catch (Exception e) {
            log.error("添加人脸特征到 Hazelcast 异常: faceId={}, error={}", faceId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取数据库中人脸特征的总数
     */
    public long getTotalFaceFeatureCount() {
        QueryWrapper<BosgFaceFturD> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("VALI_FLAG", "1")
                   .eq("FACE_TMPL_STAS", "1")
                   .isNotNull("FACE_FTUR_DATA");
        
        return bosgFaceFturDMapper.selectCount(queryWrapper);
    }
}

