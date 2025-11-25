package com.biometric.algo.service;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.dto.*;
import com.biometric.algo.socket.AlgoSocketClient;
import com.biometric.algo.strategy.ComparisonStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.biometric.algo.dto.AlgoCommand.*;

/**
 * 人脸算法核心服务
 * 封装具体的算法指令调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceAlgoService {

    private final AlgoSocketConfig config;
    private final AlgoSocketClient socketClient;
    private final Map<String, ComparisonStrategy> strategyMap;

    // ==================== 1. 人脸比对 (1:1) ====================
    public SocketRecogResult compare(String strategyType, JSONObject data1, JSONObject data2) {
        ComparisonStrategy strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            throw new IllegalArgumentException("未找到支持的比对策略: " + strategyType);
        }
        return strategy.compare(data1, data2, config.getDefaultFaceVersion());
    }

    public SocketRecogResult faceCompareFeatToFeat(JSONObject featureMap1, JSONObject featureMap2) {
        return compare("FEAT_TO_FEAT", featureMap1, featureMap2);
    }

    public SocketRecogResult faceCompareFeatToImg(JSONObject featureMap1, JSONObject imageMap2) {
        return compare("FEAT_TO_IMG", featureMap1, imageMap2);
    }

    public SocketRecogResult faceCompareImgToImg(JSONObject imageMap1, JSONObject imageMap2) {
        return compare("IMG_TO_IMG", imageMap1, imageMap2);
    }

    // ==================== 2. 特征提取 ====================

    /**
     * Y01.00 标准特征提取
     */
    public SocketFaceFeature faceExtractFeature(JSONObject images) {
        return this.faceExtractFeature(images, true, false);
    }
    public SocketFaceFeature faceExtractFeature(JSONObject images, boolean rotate, boolean needQuality) {
        return socketClient.execute(
                AlgoRequest.builder()
                        .command(EXTRACT_FEATURE)
                        .version(config.getDefaultFaceVersion())
                        .build()
                        .addParam(KEY_IMAGES, images)
                        .addParam(KEY_NUM, images.size())
                        .addParam("ROTATE", rotate)
                        .addParam(KEY_QUALITY, needQuality),
                SocketFaceFeature.class
        );
    }

    /**
     * Y01.01 移动端/带坐标特征提取
     */
    public SocketFaceFeature faceExtractMobile(JSONObject images){
        return this.faceExtractMobile(images, null, false);
    }
    public SocketFaceFeature faceExtractMobile(JSONObject images, JSONObject facesRect, boolean needQuality) {
        AlgoRequest request = AlgoRequest.builder()
                .command(EXTRACT_MOBILE)
                .version(config.getDefaultFaceVersion())
                .build()
                .addParam(KEY_IMAGES, images)
                .addParam(KEY_NUM, images.size())
                .addParam(KEY_QUALITY, needQuality);

        if (facesRect != null && !facesRect.isEmpty()) {
            request.addParam(KEY_FACES, facesRect);
        }

        return socketClient.execute(request, SocketFaceFeature.class);
    }

    /**
     * Y01.02 多人脸特征提取
     */
    public SocketMultiFaceFeature faceExtractMultiFace(String imageBase64, boolean needQuality) {
        // 业务特定逻辑：Y01.02 接口在基础版本为 FACE310 时，需切换特定版本号
        String version = resolveMultiFaceVersion(config.getDefaultFaceVersion());

        return socketClient.execute(
                AlgoRequest.builder()
                        .command(EXTRACT_MULTI)
                        .version(version)
                        .build()
                        .addParam("PIMAGE", imageBase64)
                        .addParam(KEY_QUALITY, needQuality),
                SocketMultiFaceFeature.class
        );
    }

    // ==================== 3. 图像处理与检测 ====================

    /**
     * Y03.00/Y03.01 人脸裁剪
     */
    public SocketImageProcessResult faceCrop(JSONObject images, int width, int height, boolean stdImg, JSONObject thresholds) {
        boolean useQuality = (thresholds != null && !thresholds.isEmpty());
        AlgoCommand command = useQuality ? FACE_CROP_WITH_QUALITY : FACE_CROP;

        AlgoRequest request = AlgoRequest.builder()
                .command(command)
                .version(VERSION_QUALITY)
                .build()
                .addParam(KEY_IMAGES, images)
                .addParam(KEY_NUM, images.size())
                .addParam("WIDTH", width)
                .addParam("HEIGHT", height);

        if (useQuality) {
            request.getParams().putAll(thresholds);
        } else {
            request.addParam("STDIMG", stdImg ? 1 : 0);
        }

        return socketClient.execute(request, SocketImageProcessResult.class);
    }

    /**
     * Y03.02 去网格
     */
    public SocketImageProcessResult imageRemoveGrid(JSONObject images) {
        return socketClient.execute(
                createQualityRequest(IMAGE_REMOVE_GRID, images),
                SocketImageProcessResult.class
        );
    }

    /**
     * Y03.03 人脸检测
     */
    public SocketFaceDetectionResult faceDetect(JSONObject images) {
        return socketClient.execute(
                createQualityRequest(FACE_DETECT, images),
                SocketFaceDetectionResult.class
        );
    }

    /**
     * Y03.04 人脸质量评估
     */
    public SocketFaceDetectResult faceQualityCheck(JSONObject images, JSONObject facesRect) {
        AlgoRequest request = createQualityRequest(QUALITY_CHECK, images);
        if (facesRect != null && !facesRect.isEmpty()) {
            request.addParam(KEY_FACES, facesRect);
        }
        return socketClient.execute(request, SocketFaceDetectResult.class);
    }

    private AlgoRequest createQualityRequest(AlgoCommand command, JSONObject images) {
        return AlgoRequest.builder()
                .command(command)
                .version(VERSION_QUALITY)
                .build()
                .addParam(KEY_IMAGES, images)
                .addParam(KEY_NUM, images.size());
    }

    private String resolveMultiFaceVersion(String currentVersion) {
        if ("FACE310".equals(currentVersion)) {
            return "NXFACEA102";
        }
        return currentVersion;
    }

}