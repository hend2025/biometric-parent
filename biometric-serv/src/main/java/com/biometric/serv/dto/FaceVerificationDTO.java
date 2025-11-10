package com.biometric.serv.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 人脸验证DTO
 */
@Data
public class FaceVerificationDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "特征向量不能为空")
    private List<Float> featureVector;

    private String queryImageUrl;
}

