package com.biometric.algo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CachedFaceFeature implements Serializable {

    private String faceId;

    private String algoType;

    private String templateType;

    private byte[] featureData;

    private int[] binaryFeature;

}