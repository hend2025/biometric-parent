package com.biometric.algo.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图片处理结果响应（Y03.00/Y03.01/Y03.02）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SocketImageProcessResult extends SocketResponse<ImageProcessValue> {
    
    @JSONField(name = "RETURNVALUE")
    private ImageProcessValue returnValue;
    
    /**
     * 每张图片的处理结果详情
     * 格式：{"0": 0, "1": 0, "2": 1}
     * 0表示成功，非0表示失败错误码
     */
    @JSONField(name = "DETAIL")
    private JSONObject detail;
}
