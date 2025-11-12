package com.biometric.algo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 人脸匹配结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceMatchResult implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 人脸ID
     */
    private String faceId;

    /**
     * 用户ID
     */
    private String psnNo;

    /**
     * 相似度分数（0-1之间）
     */
    private float similarity;

    /**
     * 是否匹配成功
     */
    private Boolean matched;

}

