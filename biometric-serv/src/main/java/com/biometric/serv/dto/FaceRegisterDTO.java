package com.biometric.serv.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 人脸注册DTO
 */
@Data
public class FaceRegisterDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "特征向量不能为空")
    private List<Float> featureVector;

    @NotNull(message = "图片URL不能为空")
    private String imageUrl;

    private String remark;
}

