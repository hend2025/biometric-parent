package com.biometric.algo.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 图片处理返回值（Y03.00, Y03.01, Y03.02共用）
 */
@Data
public class ImageProcessValue {
    
    /**
     * 图片数据组
     * 格式：{"images": {"0": "base64...", "1": "base64..."}, "algtype": 1, "num": 2}
     */
    private JSONObject IMAGES;
}
