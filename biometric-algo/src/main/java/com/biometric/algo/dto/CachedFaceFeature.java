package com.biometric.algo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CachedFaceFeature implements Serializable {
    private static final long serialVersionUID = 1L;

    private String faceId;

    private String algoType;

    private String templateType;

    private byte[] featureData;

    private int[] binaryFeature;

    private float[] featureVector;

}