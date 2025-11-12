package com.biometric.algo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 人脸特征数据模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceFeature implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 人脸ID
     */
    private String faceId;

    /**
     * 人员ID
     */
    private String psnNo;

    /**
     * 人脸特征向量（128维或512维）
     */
    private byte[] featureVector;

    /**
     * 图片URL
     */
    private String imageUrl;

}

