package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * Face information data
 * Extracted from nested class for Single Responsibility Principle
 */
@Data
public class FaceInfo {
    
    @JSONField(name = "blurredquality")
    private int blurredQuality;
    
    @JSONField(name = "blurredscore")
    private double blurredScore;
    
    @JSONField(name = "face")
    private String face;
    
    @JSONField(name = "posequality")
    private int poseQuality;
    
    @JSONField(name = "posescore")
    private double poseScore;
}
