package com.biometric.algo.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * Y03.03 人脸检测返回值
 */
@Data
public class FaceDetectionValue {
    
    /**
     * 每张图片的检测结果
     * 格式：{"0": "[{\"face\":\"x55y55w55h55\",\"points\":\"x|y,x|y\"}]", "1": "..."}
     */
    @JSONField(name = "VALUE")
    private JSONObject value;
}
