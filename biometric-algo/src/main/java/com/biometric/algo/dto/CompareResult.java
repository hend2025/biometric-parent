package com.biometric.algo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CompareResult implements Serializable {

    private String psnTmplNo;

    private String faceId;

    private float score;

    private boolean matched;

    private String maxFaceId;

    private float maxScore;

    private String minFaceId;

    private float minScore;

    private List<compareDetails> details;

    @Data
    public static class compareDetails implements Serializable {

        private String faceId1;

        private String faceId2;

        private float score;

        private boolean matched;

    }

}