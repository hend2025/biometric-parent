package com.biometric.algo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CachedFaceFeature implements Serializable {

    private String faceId;

    private String psnTmplNo;

    private String algoType;

    private String templateType;

    private String[] groupIds;

    private byte[] featureData;

    private int[] binaryFeature;

}