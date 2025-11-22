package com.biometric.algo.service;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.builder.AlgoRequestBuilder;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.dto.SocketFaceDetectResult;
import com.biometric.algo.dto.SocketFaceFeature;
import com.biometric.algo.dto.SocketRecogResult;
import com.biometric.algo.factory.ResponseFactory;
import com.biometric.algo.socket.SocketClient;
import com.biometric.algo.strategy.ComparisonStrategy;
import com.biometric.algo.strategy.FeatureToFeatureStrategy;
import com.biometric.algo.strategy.FeatureToImageStrategy;
import com.biometric.algo.strategy.ImageToImageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Refactored Socket Service with Design Patterns:
 * - Builder Pattern: AlgoRequestBuilder for clean request construction
 * - Strategy Pattern: Different comparison strategies
 * - Factory Pattern: ResponseFactory for response parsing
 * - Template Method Pattern: Base strategy classes
 * - Singleton Pattern: Spring @Service for single instance
 * - Improved Resource Management: SocketClient with try-with-resources
 * - Custom Exception Hierarchy: Specific exceptions for different errors
 * - Single Responsibility: Each class has one clear purpose
 */
@Slf4j
@Service
public class SocketServiceRefactored {
    
    private static final int ALG_TYPE_FACE_VISIBLE = 1;
    
    private final AlgoSocketConfig config;
    private final SocketClient socketClient;
    private final Map<String, ComparisonStrategy> comparisonStrategies;
    
    public SocketServiceRefactored(AlgoSocketConfig config, SocketClient socketClient) {
        this.config = config;
        this.socketClient = socketClient;
        this.comparisonStrategies = initializeStrategies();
    }
    
    /**
     * Initialize comparison strategies (Strategy Pattern)
     */
    private Map<String, ComparisonStrategy> initializeStrategies() {
        Map<String, ComparisonStrategy> strategies = new HashMap<>();
        strategies.put("FEAT_TO_FEAT", new FeatureToFeatureStrategy(socketClient));
        strategies.put("FEAT_TO_IMG", new FeatureToImageStrategy(socketClient));
        strategies.put("IMG_TO_IMG", new ImageToImageStrategy(socketClient));
        return strategies;
    }
    
    // ==================== Comparison Operations ====================
    
    /**
     * 3.1 Y00.00 人脸特征比对 (特征 vs 特征)
     * @param featureMap1 特征Map {"id": "base64..."}
     * @param featureMap2 特征Map {"id": "base64..."}
     */
    public SocketRecogResult faceCompareFeatToFeat(JSONObject featureMap1, JSONObject featureMap2) {
        ComparisonStrategy strategy = comparisonStrategies.get("FEAT_TO_FEAT");
        return strategy.compare(featureMap1, featureMap2, config.getDefaultFaceVersion());
    }
    
    /**
     * 3.2 Y00.01 人脸特征比对 (特征 vs 图片组)
     */
    public SocketRecogResult faceCompareFeatToImg(JSONObject featureMap1, JSONObject imageMap2) {
        ComparisonStrategy strategy = comparisonStrategies.get("FEAT_TO_IMG");
        return strategy.compare(featureMap1, imageMap2, config.getDefaultFaceVersion());
    }
    
    /**
     * 3.3 Y00.02 人脸特征比对 (图片组 vs 图片组)
     */
    public SocketRecogResult faceCompareImgToImg(JSONObject imageMap1, JSONObject imageMap2) {
        ComparisonStrategy strategy = comparisonStrategies.get("IMG_TO_IMG");
        return strategy.compare(imageMap1, imageMap2, config.getDefaultFaceVersion());
    }
    
    // ==================== Feature Extraction ====================
    
    /**
     * 3.4 Y01.00 人脸特征提取 (JPG照片)
     * @param images 图片Map {"id": "base64..."}
     */
    public SocketFaceFeature faceExtractFeature(JSONObject images) {
        return faceExtractFeature(images, true, false);
    }
    
    /**
     * 3.4 Y01.00 人脸特征提取 (JPG照片) - 完整参数
     */
    public SocketFaceFeature faceExtractFeature(JSONObject images, boolean rotate, boolean needQuality) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y01.00")
                .images(images)
                .imageNum(images.size())
                .algType(String.valueOf(ALG_TYPE_FACE_VISIBLE))
                .rotate(rotate)
                .quality(needQuality)
                .version(config.getDefaultFaceVersion())
                .build();
        
        String jsonResponse = socketClient.sendRequest(params);
        return ResponseFactory.parseFaceFeature(jsonResponse);
    }
    
    /**
     * 3.5 Y01.01 人脸特征提取 (移动终端，带人脸框)
     */
    public SocketFaceFeature faceExtractMobile(JSONObject images, JSONObject facesRect, boolean needQuality) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y01.01")
                .images(images)
                .faces(facesRect)
                .imageNum(images.size())
                .algType(String.valueOf(ALG_TYPE_FACE_VISIBLE))
                .quality(needQuality)
                .version(config.getDefaultFaceVersion())
                .build();
        
        String jsonResponse = socketClient.sendRequest(params);
        return ResponseFactory.parseFaceFeature(jsonResponse);
    }
    
    /**
     * 3.6 Y01.02 人脸特征提取 (多人脸照片)
     * 注意：此接口输入是单张大图的Base64
     */
    public String faceExtractMultiFace(String imageBase64, boolean needQuality) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y01.02")
                .pImage(imageBase64)
                .algType(ALG_TYPE_FACE_VISIBLE)
                .quality(needQuality)
                .version(config.getDefaultFaceVersion())
                .build();
        
        return socketClient.sendRequest(params);
    }
    
    // ==================== Face Processing ====================
    
    /**
     * 6.1 Y03.00 人脸裁剪 (标准裁剪)
     */
    public String faceCrop(JSONObject imagesMap, int width, int height, boolean stdImg) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y03.00")
                .images(imagesMap)
                .imageNum(imagesMap.size())
                .algType("1")
                .version("QUALITY")
                .width(width)
                .height(height)
                .stdImg(stdImg)
                .build();
        
        return socketClient.sendRequest(params);
    }
    
    /**
     * 6.2 Y03.01 人脸裁剪 (带质量评估及阈值控制)
     */
    public String faceCropWithQuality(JSONObject imagesMap, int width, int height, JSONObject thresholds) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y03.01")
                .images(imagesMap)
                .imageNum(imagesMap.size())
                .algType("1")
                .version("QUALITY")
                .width(width)
                .height(height)
                .thresholds(thresholds)
                .build();
        
        return socketClient.sendRequest(params);
    }
    
    /**
     * 6.2 Y03.02 去网格
     */
    public String imageRemoveGrid(JSONObject imagesMap) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y03.02")
                .images(imagesMap)
                .imageNum(imagesMap.size())
                .algType("1")
                .version("QUALITY")
                .build();
        
        return socketClient.sendRequest(params);
    }
    
    /**
     * 6.3 Y03.03 人脸检测 (获取坐标和关键点)
     */
    public String faceDetect(JSONObject imagesMap) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y03.03")
                .images(imagesMap)
                .imageNum(imagesMap.size())
                .algType("1")
                .version("QUALITY")
                .build();
        
        return socketClient.sendRequest(params);
    }
    
    /**
     * 6.4 Y03.04 人脸质量评估
     */
    public SocketFaceDetectResult faceQualityCheck(JSONObject imagesMap, JSONObject facesRect) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y03.04")
                .images(imagesMap)
                .faces(facesRect)
                .imageNum(imagesMap.size())
                .algType("1")
                .version("QUALITY")
                .build();
        
        String jsonResponse = socketClient.sendRequest(params);
        return ResponseFactory.parseFaceDetect(jsonResponse);
    }
}
