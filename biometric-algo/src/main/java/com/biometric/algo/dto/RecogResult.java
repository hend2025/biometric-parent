package com.biometric.algo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RecogResult implements Serializable {

    private String psnTmplNo;

    private String faceId;

    private float score;

    private boolean matched;

    private String maxFaceId;

    private float maxScore;

    private String minFaceId;

    private float minScore;

    private ComparatorScore comparatorScore;

}