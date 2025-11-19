package com.biometric.serv.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("bosg_face_ftur_d")
public class FaceFtur implements Serializable {
    @TableId
    private String faceBosgId;
    private String psnTmplNo;
    private byte[] faceFturData;
}