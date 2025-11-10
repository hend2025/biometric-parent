package com.biometric.serv.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 人脸识别DTO
 */
@Data
public class FaceRecognitionDTO {

    @NotNull(message = "特征向量不能为空")
    private List<Float> featureVector;

    private String queryImageUrl;
}

