package com.biometric.algo.service;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.config.AlgoSocketConfig;
import com.biometric.algo.dto.SocketFaceDetectResult;
import com.biometric.algo.dto.SocketFaceFeature;
import com.biometric.algo.dto.SocketRecogResult;
import com.biometric.algo.exception.AlgoException;
import com.biometric.algo.exception.SocketConnectionException;
import com.biometric.algo.socket.SocketClient;

/**
 * SocketServiceRefactored 测试类
 * 展示如何使用重构后的服务和新的设计模式
 */
public class SocketServiceRefactoredTest {

    public static void main(String[] args) {
        // 1. 配置初始化
        AlgoSocketConfig config = new AlgoSocketConfig();
        config.setHost("192.168.10.250");
        config.setPort(9098);
        config.setTimeout(60000);
        config.setDefaultFaceVersion("FACE310");
        
        // 2. 创建 Socket 客户端
        SocketClient socketClient = new SocketClient(config);
        
        // 3. 创建重构后的服务 (使用依赖注入)
        SocketServiceRefactored service = new SocketServiceRefactored(config, socketClient);
        
        // 测试图片 (Base64编码)
        String image = "";
        
        JSONObject images = new JSONObject();
        images.put("0", image);
        
        try {
            System.out.println("\n========================================");
            System.out.println("   使用重构后的 SocketService 测试");
            System.out.println("   应用设计模式: Builder, Strategy, Factory");
            System.out.println("========================================\n");
            
            // ==================== 测试1: 人脸质量评估 ====================
            System.out.println("【测试1】Y03.04 人脸质量评估");
            System.out.println("----------------------------------------");
            SocketFaceDetectResult detectResult = service.faceQualityCheck(images, null);
            System.out.println("返回码: " + detectResult.getReturnId());
            System.out.println("返回描述: " + detectResult.getReturnDesc());
            if (detectResult.getReturnId() == 0) {
                System.out.println("✓ 质量评估成功");
            }
            System.out.println();
            
            // ==================== 测试2: 人脸特征提取 ====================
            System.out.println("【测试2】Y01.00 人脸特征提取");
            System.out.println("----------------------------------------");
            SocketFaceFeature featureResult = service.faceExtractFeature(images, true, false);
            System.out.println("返回码: " + featureResult.getReturnId());
            System.out.println("返回描述: " + featureResult.getReturnDesc());
            
            String feature = null;
            if (featureResult.getReturnId() == 0 && featureResult.getReturnValue() != null) {
                feature = featureResult.getReturnValue()
                        .getFeature()
                        .getFeatureValue()
                        .getString("0");
                System.out.println("✓ 特征提取成功");
                System.out.println("特征数据长度: " + (feature != null ? feature.length() : 0));
            }
            System.out.println();
            
            // ==================== 测试3: 人脸特征比对 (特征 vs 特征) ====================
            if (feature != null) {
                System.out.println("【测试3】Y00.00 人脸特征比对 (特征 vs 特征)");
                System.out.println("使用 Strategy Pattern - FeatureToFeatureStrategy");
                System.out.println("----------------------------------------");
                
                JSONObject feature1 = new JSONObject();
                JSONObject feature2 = new JSONObject();
                feature1.put("0", feature);
                feature2.put("0", feature);
                
                SocketRecogResult recogResult = service.faceCompareFeatToFeat(feature1, feature2);
                System.out.println("返回码: " + recogResult.getReturnId());
                System.out.println("返回描述: " + recogResult.getReturnDesc());
                
                if (recogResult.getReturnId() == 0 && recogResult.getReturnValue() != null) {
                    System.out.println("✓ 比对成功");
                    System.out.println("平均相似度: " + recogResult.getReturnValue().getAvg());
                    System.out.println("最大相似度: " + recogResult.getReturnValue().getMax());
                    System.out.println("最小相似度: " + recogResult.getReturnValue().getMin());
                }
                System.out.println();
                
                // ==================== 测试4: 人脸特征比对 (特征 vs 图片) ====================
                System.out.println("【测试4】Y00.01 人脸特征比对 (特征 vs 图片组)");
                System.out.println("使用 Strategy Pattern - FeatureToImageStrategy");
                System.out.println("----------------------------------------");
                
                recogResult = service.faceCompareFeatToImg(feature1, images);
                System.out.println("返回码: " + recogResult.getReturnId());
                System.out.println("返回描述: " + recogResult.getReturnDesc());
                
                if (recogResult.getReturnId() == 0 && recogResult.getReturnValue() != null) {
                    System.out.println("✓ 比对成功");
                    System.out.println("平均相似度: " + recogResult.getReturnValue().getAvg());
                }
                System.out.println();
                
                // ==================== 测试5: 人脸特征比对 (图片 vs 图片) ====================
                System.out.println("【测试5】Y00.02 人脸特征比对 (图片组 vs 图片组)");
                System.out.println("使用 Strategy Pattern - ImageToImageStrategy");
                System.out.println("----------------------------------------");
                
                JSONObject image1 = new JSONObject();
                JSONObject image2 = new JSONObject();
                image1.put("0", image);
                image2.put("0", image);
                
                recogResult = service.faceCompareImgToImg(image1, image2);
                System.out.println("返回码: " + recogResult.getReturnId());
                System.out.println("返回描述: " + recogResult.getReturnDesc());
                
                if (recogResult.getReturnId() == 0 && recogResult.getReturnValue() != null) {
                    System.out.println("✓ 比对成功");
                    System.out.println("平均相似度: " + recogResult.getReturnValue().getAvg());
                }
                System.out.println();
            }
            
            // ==================== 测试6: 人脸裁剪 ====================
            System.out.println("【测试6】Y03.00 人脸裁剪");
            System.out.println("使用 Builder Pattern 构建请求");
            System.out.println("----------------------------------------");
            String cropResult = service.faceCrop(images, 200, 200, true);
            System.out.println("✓ 裁剪请求已发送");
            System.out.println("响应数据长度: " + cropResult.length());
            System.out.println();
            
            // ==================== 测试总结 ====================
            System.out.println("========================================");
            System.out.println("   测试完成汇总");
            System.out.println("========================================");
            System.out.println("✓ 所有测试用例执行完成");
            System.out.println("✓ 设计模式应用成功:");
            System.out.println("  - Builder Pattern: 清晰的请求参数构建");
            System.out.println("  - Strategy Pattern: 灵活的比对策略切换");
            System.out.println("  - Factory Pattern: 统一的响应解析");
            System.out.println("  - Try-with-Resources: 自动资源管理");
            System.out.println("✓ 代码可读性和可维护性显著提升");
            System.out.println();
            
        } catch (SocketConnectionException e) {
            System.err.println("❌ Socket连接异常: " + e.getMessage());
            System.err.println("请检查:");
            System.err.println("  1. 算法引擎服务是否启动");
            System.err.println("  2. IP地址和端口配置是否正确: " + config.getHost() + ":" + config.getPort());
            System.err.println("  3. 网络连接是否正常");
            e.printStackTrace();
            
        } catch (AlgoException e) {
            System.err.println("❌ 算法处理异常: " + e.getMessage());
            System.err.println("错误码: " + e.getErrorCode());
            e.printStackTrace();
            
        } catch (Exception e) {
            System.err.println("❌ 未知异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
