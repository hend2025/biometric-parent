package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Map;

/**
 * Face feature value data
 * Extracted from nested class for Single Responsibility Principle
 */
@Data
public class FaceFeatureValue {
    
    @JSONField(name = "MINSCORE")
    private double minScore;
    
    @JSONField(name = "DETAIL")
    private Map<String, Integer> detail;
    
    @JSONField(name = "ATTACHMENT")
    private Map<String, FaceFeatureAttachment> attachment;
    
    @JSONField(name = "FEATURE")
    private FeatureData feature;
}
