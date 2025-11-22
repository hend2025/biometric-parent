package com.biometric.algo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.dto.*;
import com.biometric.algo.socket.AlgoSocketClient;
import com.biometric.algo.strategy.*;
import com.biometric.algo.util.ImageToBase64Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FaceAlgoServiceTest {

    public static void main(String[] args) {
        try {
            // ==================== 1. 初始化配置与服务 ====================
            AlgoSocketConfig config = new AlgoSocketConfig();
            config.setHost("192.168.10.250");
            config.setPort(9098);
            config.setTimeout(60000);
            config.setDefaultFaceVersion("FACE310");

            AlgoSocketClient client = new AlgoSocketClient(config);

            // 初始化策略 Map
            Map<String, ComparisonStrategy> strategies = new HashMap<>();
            strategies.put("FEAT_TO_FEAT", new FeatureToFeatureStrategy(client));
            strategies.put("FEAT_TO_IMG", new FeatureToImageStrategy(client));
            strategies.put("IMG_TO_IMG", new ImageToImageStrategy(client));

            FaceAlgoService service = new FaceAlgoService(client, config, strategies);

            System.out.println("服务初始化完成...");

            // ==================== 2. 准备测试数据 ====================
            String imageBase64 = ImageToBase64Util.convertImageToBase64("D:\\dt.jpg");

            // 基础图片对象 (用于单图接口)
            JSONObject images = new JSONObject();
            images.put("0", imageBase64);

            String extractedFeature = null;

            // ==================== 测试 1: 标准特征提取 (Y01.00) ====================
            System.out.println("\n【测试 1】Y01.00 标准特征提取");
            SocketFaceFeature featureResult = service.faceExtractFeature(images, true, false);
            System.out.println("提取结果: " + featureResult.getReturnDesc());

            if (featureResult.getReturnId() == 0 && featureResult.getReturnValue() != null) {
                extractedFeature = featureResult.getReturnValue().getFeature().getFeatureValue().getString("0");
                System.out.println("✓ 成功提取特征，长度: " + extractedFeature.length());
            } else {
                System.err.println("✗ 特征提取失败，部分后续测试可能受影响");
            }

            // ==================== 测试 2: 移动端特征提取 (Y01.01) ====================
            System.out.println("\n【测试 2】Y01.01 移动端特征提取");
            SocketFaceFeature mobileResult = service.faceExtractMobile(images, null, true);
            System.out.println("移动端提取结果: " + mobileResult.getReturnDesc());
            if (mobileResult.getReturnId() == 0) {
                System.out.println("✓ 成功");
            }

            // ==================== 测试 3: 多人脸特征提取 (Y01.02) ====================
            System.out.println("\n【测试 3】Y01.02 多人脸特征提取");
            SocketMultiFaceFeature multiFaceResult = service.faceExtractMultiFace(imageBase64, true);
            System.out.println("多人脸提取结果: " + multiFaceResult.getReturnDesc());
            if (multiFaceResult.getReturnId() == 0 && multiFaceResult.getReturnValue() != null) {
                List<MultiFaceExtractResult> faces = JSON.parseArray(multiFaceResult.getReturnValue(), MultiFaceExtractResult.class);
                System.out.println("✓ 检测到人脸数: " + (faces != null ? faces.size() : 0));
            }

            // ==================== 测试 4: 特征 vs 特征比对 (Y00.00) ====================
            if (extractedFeature != null) {
                System.out.println("\n【测试 4】Y00.00 特征 vs 特征比对");

                // 参照实现：分别构建两个特征对象
                JSONObject feature1 = new JSONObject();
                JSONObject feature2 = new JSONObject();
                feature1.put("0", extractedFeature);
                feature2.put("0", extractedFeature);

                SocketRecogResult compareResult = service.faceCompareFeatToFeat(feature1, feature2);
                System.out.println("比对结果: " + compareResult.getReturnDesc());
                if (compareResult.getReturnId() == 0) {
                    System.out.println("✓ 相似度得分: " + compareResult.getReturnValue().getMax());
                }
            }

            // ==================== 测试 5: 特征 vs 图片比对 (Y00.01) ====================
            if (extractedFeature != null) {
                System.out.println("\n【测试 5】Y00.01 特征 vs 图片比对");

                // 分别构建特征组和图片组
                JSONObject featureMap = new JSONObject();
                featureMap.put("0", extractedFeature);

                JSONObject imageMap = new JSONObject();
                imageMap.put("0", imageBase64);

                SocketRecogResult featToImgResult = service.faceCompareFeatToImg(featureMap, imageMap);
                System.out.println("比对结果: " + featToImgResult.getReturnDesc());
                if (featToImgResult.getReturnId() == 0) {
                    System.out.println("✓ 平均相似度: " + featToImgResult.getReturnValue().getAvg());
                }
            }

            // ==================== 测试 6: 图片 vs 图片比对 (Y00.02) ====================
            System.out.println("\n【测试 6】Y00.02 图片 vs 图片比对");

            // 【优化】参照特征比对，分别实例化 image1 和 image2
            JSONObject image1 = new JSONObject();
            JSONObject image2 = new JSONObject();
            image1.put("0", imageBase64);
            image2.put("0", imageBase64);

            SocketRecogResult imgToImgResult = service.faceCompareImgToImg(image1, image2);
            System.out.println("比对结果: " + imgToImgResult.getReturnDesc());
            if (imgToImgResult.getReturnId() == 0) {
                System.out.println("✓ 平均相似度: " + imgToImgResult.getReturnValue().getAvg());
            }

            // ==================== 测试 7: 标准人脸裁剪 (Y03.00) ====================
            System.out.println("\n【测试 7】Y03.00 标准人脸裁剪");
            SocketImageProcessResult cropResult = service.faceCrop(images, 200, 200, true, null);
            System.out.println("裁剪结果: " + cropResult.getReturnDesc());
            if (cropResult.getReturnId() == 0) {
                ImageProcessReturnValue val = JSON.parseObject(cropResult.getReturnValue(), ImageProcessReturnValue.class);
                System.out.println("✓ 裁剪后图片数: " + val.getIMAGES().getNum());
            }

            // ==================== 测试 8: 带质量控制的裁剪 (Y03.01) ====================
            System.out.println("\n【测试 8】Y03.01 带质量控制的裁剪");
            JSONObject thresholds = new JSONObject();
            thresholds.put("MULTI", 1);
            thresholds.put("MINDETECTSIZE", 50);
            thresholds.put("BLURREDTHRESHOLD", 0.8);

            SocketImageProcessResult qualityCropResult = service.faceCrop(images, 200, 200, false, thresholds);
            System.out.println("质量裁剪结果: " + qualityCropResult.getReturnDesc());
            if (qualityCropResult.getReturnId() == 0) {
                System.out.println("✓ 成功");
            }

            // ==================== 测试 9: 图片去网格 (Y03.02) ====================
            System.out.println("\n【测试 9】Y03.02 图片去网格");
            SocketImageProcessResult gridResult = service.imageRemoveGrid(images);
            System.out.println("去网格结果: " + gridResult.getReturnDesc());
            if (gridResult.getReturnId() == 0) {
                System.out.println("✓ 成功");
            }

            // ==================== 测试 10: 人脸检测 (Y03.03) ====================
            System.out.println("\n【测试 10】Y03.03 人脸检测");
            SocketFaceDetectionResult detectResult = service.faceDetect(images);
            System.out.println("检测结果: " + detectResult.getReturnDesc());
            if (detectResult.getReturnId() == 0) {
                FaceDetectionValue val = JSON.parseObject(detectResult.getReturnValue(), FaceDetectionValue.class);
                System.out.println("✓ 检测到图片数: " + (val.getValue() != null ? val.getValue().size() : 0));
            }

            // ==================== 测试 11: 质量评估 (Y03.04) ====================
            System.out.println("\n【测试 11】Y03.04 人脸质量评估");
            SocketFaceDetectResult qualityCheckResult = service.faceQualityCheck(images, null);
            System.out.println("评估结果: " + qualityCheckResult.getReturnDesc());
            if (qualityCheckResult.getReturnId() == 0) {
                System.out.println("✓ 成功");
            }

            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}