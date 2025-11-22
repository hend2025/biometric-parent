package com.biometric.algo.dto;

import lombok.Data;

/**
 * Y01.02 多人脸特征提取单个人脸结果
 */
@Data
public class MultiFaceExtractResult {
    
    /**
     * 人脸位置：x1852y255w121h88
     */
    private String face;
    
    /**
     * 人脸特征值（Base64编码）
     */
    private String feat;
    
    /**
     * 质量评估信息
     */
    private QualityInfo quality;
    
    /**
     * 质量评估详细信息
     */
    @Data
    public static class QualityInfo {
        /**
         * 模糊度 (0-1)
         */
        private Double blurred;
        
        /**
         * 对比度 (0-1)
         */
        private Double contrast;
        
        /**
         * 亮度 (0-1)
         */
        private Double luminance;
        
        /**
         * 俯仰角
         */
        private Double pitch;
        
        /**
         * 翻滚角
         */
        private Double roll;
        
        /**
         * 均匀度
         */
        private Double uniform;
        
        /**
         * 偏航角
         */
        private Double yaw;
    }
}
