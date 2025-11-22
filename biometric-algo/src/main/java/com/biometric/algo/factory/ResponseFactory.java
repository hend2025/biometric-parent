package com.biometric.algo.factory;

import com.alibaba.fastjson.JSON;
import com.biometric.algo.dto.SocketFaceDetectResult;
import com.biometric.algo.dto.SocketFaceFeature;
import com.biometric.algo.dto.SocketRecogResult;
import com.biometric.algo.dto.SocketResponse;
import com.biometric.algo.exception.AlgoProcessException;

/**
 * Factory Pattern: 统一处理响应解析和验证
 * 所有方法遵循相同的处理流程：
 * 1. JSON解析
 * 2. 验证返回码
 * 3. 异常处理统一化
 */
public class ResponseFactory {
    
    /**
     * 解析人脸比对结果
     * @param jsonResponse JSON响应字符串
     * @return 比对结果对象
     */
    public static SocketRecogResult parseRecogResult(String jsonResponse) {
        return parseResponse(jsonResponse, SocketRecogResult.class);
    }
    
    /**
     * 解析人脸特征提取结果
     * @param jsonResponse JSON响应字符串
     * @return 特征提取结果对象
     */
    public static SocketFaceFeature parseFaceFeature(String jsonResponse) {
        return parseResponse(jsonResponse, SocketFaceFeature.class);
    }
    
    /**
     * 解析人脸检测结果
     * @param jsonResponse JSON响应字符串
     * @return 人脸检测结果对象
     */
    public static SocketFaceDetectResult parseFaceDetect(String jsonResponse) {
        return parseResponse(jsonResponse, SocketFaceDetectResult.class);
    }
    
    /**
     * 通用响应解析方法（核心逻辑）
     * 使用泛型消除重复代码
     * @param jsonResponse JSON响应字符串
     * @param clazz 目标类型
     * @return 解析后的响应对象
     */
    private static <T extends SocketResponse<?>> T parseResponse(String jsonResponse, Class<T> clazz) {
        try {
            T result = JSON.parseObject(jsonResponse, clazz);
            validateResponse(result);
            return result;
        } catch (AlgoProcessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            // 解析异常返回错误对象
            try {
                T errorResult = clazz.newInstance();
                errorResult.setReturnId(-1);
                errorResult.setReturnDesc("解析响应失败: " + e.getMessage());
                return errorResult;
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new AlgoProcessException("无法创建错误结果对象", ex);
            }
        }
    }
    
    /**
     * 验证响应结果，returnId != 0 时抛出异常
     * @param response 响应对象
     * @throws AlgoProcessException 当返回码不为0时
     */
    private static void validateResponse(SocketResponse<?> response) {
        if (response.getReturnId() != 0) {
            throw new AlgoProcessException(
                    response.getReturnId(), 
                    response.getReturnDesc());
        }
    }
}
