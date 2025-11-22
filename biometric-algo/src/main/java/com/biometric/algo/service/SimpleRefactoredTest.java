package com.biometric.algo.service;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.dto.SocketFaceDetectResult;
import com.biometric.algo.dto.SocketFaceFeature;
import com.biometric.algo.dto.SocketRecogResult;
import com.biometric.algo.socket.SocketClient;

/**
 * 简化版测试 - 更接近原始 main 方法的结构
 * 展示如何快速替换旧版代码
 */
public class SimpleRefactoredTest {

    public static void main(String[] args) {
        // ========== 初始化（与旧版类似）==========
        AlgoSocketConfig config = new AlgoSocketConfig();
        config.setHost("192.168.10.250");
        config.setPort(9098);
        
        // 创建服务（使用新的重构版本）
        SocketClient socketClient = new SocketClient(config);
        SocketServiceRefactored client = new SocketServiceRefactored(config, socketClient);

        // ========== 测试图片数据 ==========
        String image = "";
        
        JSONObject images = new JSONObject();
        images.put("0", image);

        System.out.println();
        
        // ========== Y03.04 人脸质量评估 ==========
        System.out.println("--- Y03.04 人脸质量评估 ---");
        SocketFaceDetectResult faceDetectResult = client.faceQualityCheck(images, null);
        System.out.println(faceDetectResult);

        // ========== Y01.00 特征提取 ==========
        System.out.println("--- 调用 Y01.00 特征提取 ---");
        SocketFaceFeature faceFeature = client.faceExtractFeature(images);
        String feature = faceFeature.getReturnValue().getFeature().getFeatureValue().getString("0");
        System.out.println(faceFeature);

        // ========== Y00.00 人脸特征比对 (特征 vs 特征) ==========
        System.out.println("--- Y00.00 人脸特征比对 (特征 vs 特征) ---");
        JSONObject Feat1 = new JSONObject();
        JSONObject Feat2 = new JSONObject();
        Feat1.put("0", feature);
        Feat2.put("0", feature);
        SocketRecogResult recogResult = client.faceCompareFeatToFeat(Feat1, Feat2);
        System.out.println(recogResult);

        // ========== Y00.01 人脸特征比对 (特征 vs 图片组) ==========
        System.out.println("--- Y00.01 人脸特征比对 (特征 vs 图片组) ---");
        recogResult = client.faceCompareFeatToImg(Feat1, images);
        System.out.println(recogResult);

        // ========== Y00.02 人脸特征比对 (图片组 vs 图片组) ==========
        System.out.println("--- Y00.02 人脸特征比对 (图片组 vs 图片组) ---");
        JSONObject image1 = new JSONObject();
        JSONObject image2 = new JSONObject();
        image1.put("0", image);
        image2.put("0", image);
        recogResult = client.faceCompareImgToImg(image1, image2);
        System.out.println(recogResult);
        System.out.println();
    }
}
