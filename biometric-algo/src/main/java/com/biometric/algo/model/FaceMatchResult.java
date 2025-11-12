package com.biometric.algo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceMatchResult implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String faceId;

    private String psnNo;

    private float similarity;

    private Boolean matched;

}
