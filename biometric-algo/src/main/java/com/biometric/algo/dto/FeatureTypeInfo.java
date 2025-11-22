package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * Feature type information
 * Extracted from nested class for Single Responsibility Principle
 */
@Data
public class FeatureTypeInfo {
    
    @JSONField(name = "direction")
    private int direction;
    
    @JSONField(name = "machine")
    private int machine;
}
