package com.biometric.algo.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Y03.03 人脸检测响应
 * 注意：RETURNVALUE是JSON字符串，需要手动解析
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SocketFaceDetectionResult extends SocketResponse<String> {
    
    @JSONField(name = "RETURNVALUE")
    private String returnValue;
    
    /**
     * 每张图片的处理结果详情
     * 格式：{"0": 0, "1": 0, "2": 1}
     */
    @JSONField(name = "DETAIL")
    private JSONObject detail;
}
