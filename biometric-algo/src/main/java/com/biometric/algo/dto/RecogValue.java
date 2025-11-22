package com.biometric.algo.dto;

import lombok.Data;

import java.util.Map;

/**
 * 人脸比对结果数据
 * 包含比对相似度的平均值、最大值、最小值及详细信息
 * 
 * @author biometric-algo
 * @version 1.0
 */
@Data
public class RecogValue {
    
    /** 平均相似度 */
    private double avg;
    
    /** 最大相似度 */
    private double max;
    
    /** 最小相似度 */
    private double min;
    
    /** 最大相似度对应的图片ID详情 */
    private Map<String, Integer> maxDetail;
    
    /** 最小相似度对应的图片ID详情 */
    private Map<String, Integer> minDetail;
}
