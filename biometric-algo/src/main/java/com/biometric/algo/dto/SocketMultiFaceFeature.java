package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Y01.02 多人脸特征提取响应
 * 注意：RETURNVALUE是JSON字符串，需要手动解析
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SocketMultiFaceFeature extends SocketResponse<String> {
    
    @JSONField(name = "RETURNVALUE")
    private String returnValue;
}
