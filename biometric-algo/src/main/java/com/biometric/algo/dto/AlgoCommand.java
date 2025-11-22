package com.biometric.algo.dto;

import lombok.Getter;

/**
 * 算法命令枚举
 * 定义所有支持的算法接口命令及其功能ID和描述
 */
@Getter
public enum AlgoCommand {
    // 比对类
    COMPARE_FEAT_TO_FEAT("Y00.00", "人脸特征比对(特征vs特征)"),
    COMPARE_FEAT_TO_IMG("Y00.01", "人脸特征比对(特征vs图片)"),
    COMPARE_IMG_TO_IMG("Y00.02", "人脸特征比对(图片vs图片)"),

    // 提取与检测类
    EXTRACT_FEATURE("Y01.00", "人脸特征提取"),
    EXTRACT_MOBILE("Y01.01", "移动端人脸提取"),
    EXTRACT_MULTI("Y01.02", "多人脸提取"),

    // 图像处理类
    FACE_CROP("Y03.00", "人脸裁剪"),
    FACE_CROP_WITH_QUALITY("Y03.01", "人脸裁剪(带质量评估)"),
    IMAGE_REMOVE_GRID("Y03.02", "去网格"),
    FACE_DETECT("Y03.03", "人脸检测"),
    QUALITY_CHECK("Y03.04", "质量检测");

    /** 功能ID */
    private final String funId;
    /** 功能描述 */
    private final String desc;

    AlgoCommand(String funId, String desc) {
        this.funId = funId;
        this.desc = desc;
    }

}