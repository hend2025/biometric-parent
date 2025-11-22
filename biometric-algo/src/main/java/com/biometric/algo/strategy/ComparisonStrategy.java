package com.biometric.algo.strategy;

import com.alibaba.fastjson.JSONObject;
import com.biometric.algo.dto.SocketRecogResult;

/**
 * Strategy Pattern: Interface for different comparison strategies
 */
public interface ComparisonStrategy {
    
    /**
     * Execute comparison
     * @param data1 First data object (feature or image)
     * @param data2 Second data object (feature or image)
     * @param version Algorithm version
     * @return Comparison result
     */
    SocketRecogResult compare(JSONObject data1, JSONObject data2, String version);
    
    /**
     * Get function ID for this strategy
     */
    String getFunctionId();
}
