package com.biometric.algo.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Y03.03 人脸检测响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SocketFaceDetectionResult extends SocketResponse<FaceDetectionValue> {
    
    @JSONField(name = "RETURNVALUE")
    private FaceDetectionValue returnValue;
    
    /**
     * 每张图片的处理结果详情
     * 格式：{"0": 0, "1": 0, "2": 1}
     */
    @JSONField(name = "DETAIL")
    private JSONObject detail;
}
