package com.biometric.algo.builder;

import com.alibaba.fastjson.JSONObject;

/**
 * 算法请求参数构建器
 * 采用Builder模式构建算法接口请求参数，支持链式调用
 * 
 * 主要功能：
 * - 构建各类算法接口的请求参数
 * - 支持链式调用，代码更加清晰易读
 * - 封装通用参数和特殊参数的构建方法
 * 
 * @author biometric-algo
 * @version 1.0
 */
public class AlgoRequestBuilder {
    
    /** 请求参数容器 */
    private final JSONObject params;
    
    /** 算法类型 - 可见光人脸 */
    private static final int ALG_TYPE_FACE_VISIBLE = 1;
    
    private AlgoRequestBuilder() {
        this.params = new JSONObject();
    }
    
    /**
     * 创建新的构建器实例
     * 
     * @return AlgoRequestBuilder实例
     */
    public static AlgoRequestBuilder newBuilder() {
        return new AlgoRequestBuilder();
    }
    
    /**
     * 设置功能ID
     * 
     * @param funId 功能ID（如Y01.00、Y03.01等）
     * @return 当前构建器实例
     */
    public AlgoRequestBuilder funId(String funId) {
        params.put("FUNID", funId);
        return this;
    }
    
    /**
     * 设置算法类型（整数）
     * 
     * @param algType 算法类型（1=可见光人脸）
     * @return 当前构建器实例
     */
    public AlgoRequestBuilder algType(int algType) {
        params.put("ALGTYPE", algType);
        return this;
    }
    
    /**
     * 设置算法类型（字符串）
     * 
     * @param algType 算法类型
     * @return 当前构建器实例
     */
    public AlgoRequestBuilder algType(String algType) {
        params.put("ALGTYPE", algType);
        return this;
    }
    
    /**
     * 设置算法版本号
     * 
     * @param version 版本号（如FACE310、QUALITY等）
     * @return 当前构建器实例
     */
    public AlgoRequestBuilder version(String version) {
        params.put("VERSION", version);
        return this;
    }
    
    /**
     * 设置图片数据
     * 
     * @param images 图片Map {"id": "base64..."}
     * @return 当前构建器实例
     */
    public AlgoRequestBuilder images(JSONObject images) {
        params.put("IMAGES", images);
        return this;
    }
    
    /**
     * 设置图片数量
     * 
     * @param num 图片数量
     * @return 当前构建器实例
     */
    public AlgoRequestBuilder imageNum(int num) {
        params.put("NUM", num);
        return this;
    }
    
    /**
     * 设置是否自动旋转
     * 
     * @param rotate 是否自动旋转
     * @return 当前构建器实例
     */
    public AlgoRequestBuilder rotate(boolean rotate) {
        params.put("ROTATE", rotate);
        return this;
    }
    
    /**
     * 设置是否需要质量评估
     * 
     * @param quality 是否需要质量评估
     * @return 当前构建器实例
     */
    public AlgoRequestBuilder quality(boolean quality) {
        params.put("QUALITY", quality);
        return this;
    }
    
    /**
     * 设置人脸框位置（可选）
     * 
     * @param faces 人脸框位置Map
     * @return 当前构建器实例
     */
    public AlgoRequestBuilder faces(JSONObject faces) {
        if (faces != null) {
            params.put("FACES", faces);
        }
        return this;
    }
    
    /**
     * 设置单张图片Base64（用于Y01.02多人脸提取）
     * 
     * @param imageBase64 图片Base64编码
     * @return 当前构建器实例
     */
    public AlgoRequestBuilder pImage(String imageBase64) {
        params.put("PIMAGE", imageBase64);
        return this;
    }
    
    public AlgoRequestBuilder pFeature1(JSONObject featureGroup) {
        params.put("PFEATURE1", featureGroup);
        return this;
    }
    
    public AlgoRequestBuilder pFeature2(JSONObject featureGroup) {
        params.put("PFEATURE2", featureGroup);
        return this;
    }
    
    public AlgoRequestBuilder pImage1(JSONObject imageGroup) {
        params.put("PIMAGE1", imageGroup);
        return this;
    }
    
    public AlgoRequestBuilder pImage2(JSONObject imageGroup) {
        params.put("PIMAGE2", imageGroup);
        return this;
    }
    
    public AlgoRequestBuilder width(int width) {
        params.put("WIDTH", width);
        return this;
    }
    
    public AlgoRequestBuilder height(int height) {
        params.put("HEIGHT", height);
        return this;
    }
    
    public AlgoRequestBuilder stdImg(boolean stdImg) {
        params.put("STDIMG", stdImg ? 1 : 0);
        return this;
    }
    
    public AlgoRequestBuilder thresholds(JSONObject thresholds) {
        if (thresholds != null) {
            params.putAll(thresholds);
        }
        return this;
    }
    
    /**
     * 是否裁剪多个人脸（Y03.01）
     * @param multi 1-裁剪多人脸，0-裁剪最大人脸
     */
    public AlgoRequestBuilder multi(int multi) {
        params.put("MULTI", multi);
        return this;
    }
    
    /**
     * 人脸检测框最大值（Y03.01）
     */
    public AlgoRequestBuilder maxDetectSize(int maxDetectSize) {
        params.put("MAXDETECTSIZE", maxDetectSize);
        return this;
    }
    
    /**
     * 人脸检测框最小值（Y03.01）
     */
    public AlgoRequestBuilder minDetectSize(int minDetectSize) {
        params.put("MINDETECTSIZE", minDetectSize);
        return this;
    }
    
    /**
     * 人脸检测阈值（Y03.01）
     */
    public AlgoRequestBuilder detectThreshold(int detectThreshold) {
        params.put("DETECTTHRESHOLD", detectThreshold);
        return this;
    }
    
    /**
     * 质量评估姿态阈值（Y03.01）
     */
    public AlgoRequestBuilder poseThreshold(double poseThreshold) {
        params.put("POSETHRESHOLD", poseThreshold);
        return this;
    }
    
    /**
     * 质量评估模糊度阈值（Y03.01）
     */
    public AlgoRequestBuilder blurredThreshold(double blurredThreshold) {
        params.put("BLURREDTHRESHOLD", blurredThreshold);
        return this;
    }
    
    public AlgoRequestBuilder customParam(String key, Object value) {
        params.put(key, value);
        return this;
    }
    
    public JSONObject build() {
        return params;
    }
    
    /**
     * Build data group (feature or image)
     */
    public static JSONObject buildGroup(JSONObject dataMap, int algType, String keyName) {
        JSONObject group = new JSONObject();
        group.put(keyName, dataMap);
        group.put("algtype", algType);
        group.put("num", dataMap != null ? dataMap.size() : 0);
        return group;
    }
    
    public static JSONObject buildFeatureGroup(JSONObject featureMap) {
        return buildGroup(featureMap, ALG_TYPE_FACE_VISIBLE, "feature");
    }
    
    public static JSONObject buildImageGroup(JSONObject imageMap) {
        return buildGroup(imageMap, ALG_TYPE_FACE_VISIBLE, "images");
    }
}
