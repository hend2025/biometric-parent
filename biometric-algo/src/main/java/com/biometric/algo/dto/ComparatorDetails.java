package com.biometric.algo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ComparatorDetails implements Serializable {

    private String faceId1;

    private String faceId2;

    private float score;

}
