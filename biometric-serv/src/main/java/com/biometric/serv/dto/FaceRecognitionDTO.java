package com.biometric.serv.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 人脸识别DTO
 */
@Data
public class FaceRecognitionDTO {

    private byte[] featureVector;

    private String queryImageUrl;

}

