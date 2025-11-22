package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Face detection value data
 * Extracted from nested class for Single Responsibility Principle
 */
@Data
public class FaceDetectValue {
    
    @JSONField(name = "DETAIL")
    private Map<String, Integer> detail;
    
    @JSONField(name = "VALUE")
    private Map<String, List<FaceInfo>> value;
}
