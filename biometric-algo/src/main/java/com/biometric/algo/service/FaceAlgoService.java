package com.biometric.algo.service;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.dto.*;
import com.biometric.algo.socket.AlgoSocketClient;
import com.biometric.algo.strategy.ComparisonStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 人脸算法服务
 */
@Slf4j
@Service
public class FaceAlgoService {

    private final AlgoSocketClient socketClient;
    private final AlgoSocketConfig config;
    private final Map<String, ComparisonStrategy> strategyMap;

    public FaceAlgoService(AlgoSocketClient socketClient,
                           AlgoSocketConfig config,
                           Map<String, ComparisonStrategy> strategyMap) {
        this.socketClient = socketClient;
        this.config = config;
        this.strategyMap = strategyMap;
    }

    // ==================== 1. 人脸比对 ====================

    public SocketRecogResult compare(String type, JSONObject data1, JSONObject data2) {
        ComparisonStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的比对策略: " + type);
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
    public SocketFaceFeature faceExtractFeature(JSONObject images, boolean rotate, boolean needQuality) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.EXTRACT_FEATURE)
                .version(config.getDefaultFaceVersion())
                .build();

        request.addParam("IMAGES", images)
                .addParam("NUM", images.size())
                .addParam("ROTATE", rotate)
                .addParam("QUALITY", needQuality);

        return socketClient.execute(request, SocketFaceFeature.class);
    }

    /**
     * Y01.01 移动端/带坐标特征提取
     */
    public SocketFaceFeature faceExtractMobile(JSONObject images, JSONObject facesRect, boolean needQuality) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.EXTRACT_MOBILE)
                .version(config.getDefaultFaceVersion())
                .build();

        request.addParam("IMAGES", images)
                .addParam("NUM", images.size())
                .addParam("QUALITY", needQuality);

        if (facesRect != null) {
            request.addParam("FACES", facesRect);
        }

        return socketClient.execute(request, SocketFaceFeature.class);
    }

    /**
     * Y01.02 多人脸特征提取
     * 包含特殊版本号逻辑处理
     */
    public SocketMultiFaceFeature faceExtractMultiFace(String imageBase64, boolean needQuality) {
        // 特殊业务逻辑：Y01.02 需要切换版本号
        String version = config.getDefaultFaceVersion();
        if ("FACE310".equals(version)) {
            version = "NXFACEA102";
        }

        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.EXTRACT_MULTI)
                .version(version)
                .build();

        request.addParam("PIMAGE", imageBase64)
                .addParam("QUALITY", needQuality);

        return socketClient.execute(request, SocketMultiFaceFeature.class);
    }

    // ==================== 3. 图像处理与检测 ====================

    /**
     * Y03.00/Y03.01 人脸裁剪 (整合了带阈值和不带阈值的逻辑)
     */
    public SocketImageProcessResult faceCrop(JSONObject images, int width, int height, boolean stdImg, JSONObject thresholds) {
        boolean useQuality = (thresholds != null && !thresholds.isEmpty());
        AlgoCommand command = useQuality ? AlgoCommand.FACE_CROP_WITH_QUALITY : AlgoCommand.FACE_CROP;

        AlgoRequest request = AlgoRequest.builder()
                .command(command)
                .version("QUALITY")
                .build();

        request.addParam("IMAGES", images)
                .addParam("NUM", images.size())
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
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.IMAGE_REMOVE_GRID)
                .version("QUALITY")
                .build();

        request.addParam("IMAGES", images)
                .addParam("NUM", images.size());

        return socketClient.execute(request, SocketImageProcessResult.class);
    }

    /**
     * Y03.03 人脸检测
     */
    public SocketFaceDetectionResult faceDetect(JSONObject images) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.FACE_DETECT)
                .version("QUALITY")
                .build();

        request.addParam("IMAGES", images)
                .addParam("NUM", images.size());

        return socketClient.execute(request, SocketFaceDetectionResult.class);
    }

    /**
     * Y03.04 人脸质量评估
     */
    public SocketFaceDetectResult faceQualityCheck(JSONObject images, JSONObject facesRect) {
        AlgoRequest request = AlgoRequest.builder()
                .command(AlgoCommand.QUALITY_CHECK)
                .version("QUALITY")
                .build();

        request.addParam("IMAGES", images)
                .addParam("NUM", images.size());

        if (facesRect != null) {
            request.addParam("FACES", facesRect);
        }

        return socketClient.execute(request, SocketFaceDetectResult.class);
    }

}