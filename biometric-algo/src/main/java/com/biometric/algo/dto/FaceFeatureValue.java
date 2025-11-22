package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Map;

/**
 * 人脸特征提取结果数据
 * 包含提取的特征数据、评分、详情和附加信息
 * 
 * @author biometric-algo
 * @version 1.0
 */
@Data
public class FaceFeatureValue {
    
    /** 最小评分 */
    @JSONField(name = "MINSCORE")
    private double minScore;
    
    /** 处理详情（每张图片的处理状态码） */
    @JSONField(name = "DETAIL")
    private Map<String, Integer> detail;
    
    /** 附加信息（质量评估等） */
    @JSONField(name = "ATTACHMENT")
    private Map<String, FaceFeatureAttachment> attachment;
    
    /** 特征数据 */
    @JSONField(name = "FEATURE")
    private FeatureData feature;
}
