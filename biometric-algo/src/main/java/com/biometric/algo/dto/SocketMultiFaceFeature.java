package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 多人脸特征提取响应类
 * 用于Y01.02多人脸特征提取接口的响应
 * 
 * 注意：RETURNVALUE为JSON字符串，需要手动解析为数组，每项包含face、feat、quality等信息
 * 
 * @author biometric-algo
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SocketMultiFaceFeature extends SocketResponse<String> {
    
    @JSONField(name = "RETURNVALUE")
    private String returnValue;
}
