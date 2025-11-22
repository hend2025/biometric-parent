package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * 图片处理返回值（Y03.00, Y03.01, Y03.02共用）
 */
@Data
public class ImageProcessValue {
    
    /**
     * 图片数据（JSON字符串）
     * 格式："{\"0\":\"base64...\",\"1\":\"base64...\"}"
     */
    @JSONField(name = "images")
    private String images;
    
    /**
     * 算法类型
     */
    @JSONField(name = "algtype")
    private Integer algtype;
    
    /**
     * 图片数量
     */
    @JSONField(name = "num")
    private Integer num;
}
