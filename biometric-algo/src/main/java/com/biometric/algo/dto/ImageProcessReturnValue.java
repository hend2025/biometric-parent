package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * Y03.00/01/02 返回值包装类
 */
@Data
public class ImageProcessReturnValue {
    
    @JSONField(name = "IMAGES")
    private ImageProcessValue IMAGES;
}
