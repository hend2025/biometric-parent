package com.biometric.serv.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户DTO
 */
@Data
public class UserDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private String idCard;

    private String mobile;

    private String email;

    private Integer status;
}

