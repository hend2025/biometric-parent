package com.biometric.algo.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.biometric.algo.dto.deserializer.StringToObjectDeserializer;
import lombok.Data;

import java.util.Map;

/**
 * Feature data information
 * Extracted from nested class for Single Responsibility Principle
 */
@Data
public class FeatureData {
    
    @JSONField(name = "algtype")
    private int algType;
    
    @JSONField(name = "feature", deserializeUsing = StringToObjectDeserializer.class)
    private JSONObject featureValue;
    
    @JSONField(name = "iscompress")
    private int isCompress;
    
    @JSONField(name = "iscrypt")
    private int isCrypt;
    
    @JSONField(name = "num")
    private int num;
    
    @JSONField(name = "version")
    private String version;
    
    @JSONField(name = "type")
    private Map<String, FeatureTypeInfo> type;
}
