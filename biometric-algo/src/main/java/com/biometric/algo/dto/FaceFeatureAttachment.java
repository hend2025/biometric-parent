package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * Face feature attachment information
 * Extracted from nested class for Single Responsibility Principle
 */
@Data
public class FaceFeatureAttachment {
    
    @JSONField(name = "iscolor")
    private boolean isColor;
    
    @JSONField(name = "picheight")
    private int picHeight;
    
    @JSONField(name = "picwidth")
    private int picWidth;
}
