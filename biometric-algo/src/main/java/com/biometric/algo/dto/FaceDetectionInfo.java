package com.biometric.algo.dto;

import lombok.Data;

/**
 * Y03.03 单个人脸检测信息
 */
@Data
public class FaceDetectionInfo {
    
    /**
     * 人脸位置：x55y55w55h55
     */
    private String face;
    
    /**
     * 关键点坐标：x|y,x|y,x|y,x|y
     */
    private String points;
}
