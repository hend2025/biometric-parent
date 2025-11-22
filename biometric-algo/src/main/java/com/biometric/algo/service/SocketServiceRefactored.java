package com.biometric.algo.service;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.builder.AlgoRequestBuilder;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.dto.*;
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
    public SocketFaceFeature faceExtractMobile(JSONObject images) {
        return faceExtractMobile(images,null,true);
    }

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
     * 注意：此接口输入是单张大图的Base64，返回包含多个人脸的特征数组
     * @param imageBase64 单张大图的Base64编码
     * @param needQuality 是否需要质量评估
     * @return 多人脸特征提取结果，RETURNVALUE为数组，每项包含face、feat、quality
     */
    public SocketMultiFaceFeature faceExtractMultiFace(String imageBase64, boolean needQuality) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y01.02")
                .pImage(imageBase64)
                .algType(ALG_TYPE_FACE_VISIBLE)
                .quality(needQuality)
                .version(config.getDefaultFaceVersion())
                .build();
        
        String jsonResponse = socketClient.sendRequest(params);
        return ResponseFactory.parseMultiFaceFeature(jsonResponse);
    }
    
    // ==================== Face Processing ====================
    
    /**
     * 6.1 Y03.00 人脸裁剪 (标准裁剪)
     * @param imagesMap 图片数据组
     * @param width 裁剪后图片宽度
     * @param height 裁剪后图片高度
     * @param stdImg 是否输出标准图像
     * @return 图片处理结果，RETURNVALUE包含裁剪后的图片，DETAIL包含每张图的处理状态
     */
    public SocketImageProcessResult faceCrop(JSONObject imagesMap, int width, int height, boolean stdImg) {
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
        
        String jsonResponse = socketClient.sendRequest(params);
        return ResponseFactory.parseImageProcess(jsonResponse);
    }
    
    /**
     * 6.2 Y03.01 人脸裁剪 (带质量评估及阈值控制)
     * @param imagesMap 图片数据组
     * @param width 裁剪后图片宽度
     * @param height 裁剪后图片高度
     * @param thresholds 质量评估阈值配置（可选参数：MULTI、MAXDETECTSIZE、MINDETECTSIZE等）
     * @return 图片处理结果，RETURNVALUE包含裁剪后的图片，DETAIL包含每张图的处理状态
     */
    public SocketImageProcessResult faceCropWithQuality(JSONObject imagesMap, int width, int height, JSONObject thresholds) {
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
        
        String jsonResponse = socketClient.sendRequest(params);
        return ResponseFactory.parseImageProcess(jsonResponse);
    }
    
    /**
     * 6.2 Y03.02 去网格
     * @param imagesMap 图片数据组
     * @return 图片处理结果，RETURNVALUE包含去网格后的图片，DETAIL包含每张图的处理状态
     */
    public SocketImageProcessResult imageRemoveGrid(JSONObject imagesMap) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y03.02")
                .images(imagesMap)
                .imageNum(imagesMap.size())
                .algType("1")
                .version("QUALITY")
                .build();
        
        String jsonResponse = socketClient.sendRequest(params);
        return ResponseFactory.parseImageProcess(jsonResponse);
    }
    
    /**
     * 6.3 Y03.03 人脸检测 (获取坐标和关键点)
     * @param imagesMap 图片数据组
     * @return 人脸检测结果，RETURNVALUE.VALUE包含每张图的人脸位置和关键点，DETAIL包含处理状态
     */
    public SocketFaceDetectionResult faceDetect(JSONObject imagesMap) {
        JSONObject params = AlgoRequestBuilder.newBuilder()
                .funId("Y03.03")
                .images(imagesMap)
                .imageNum(imagesMap.size())
                .algType("1")
                .version("QUALITY")
                .build();
        
        String jsonResponse = socketClient.sendRequest(params);
        return ResponseFactory.parseFaceDetection(jsonResponse);
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
