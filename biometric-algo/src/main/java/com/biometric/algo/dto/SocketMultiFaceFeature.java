package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Y01.02 多人脸特征提取响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SocketMultiFaceFeature extends SocketResponse<List<MultiFaceExtractResult>> {
    
    @JSONField(name = "RETURNVALUE")
    private List<MultiFaceExtractResult> returnValue;
}
