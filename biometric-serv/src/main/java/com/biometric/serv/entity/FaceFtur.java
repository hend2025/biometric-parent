package com.biometric.serv.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("bosg_face_ftur_d")
public class FaceFtur implements Serializable {
    @TableId("FACE_BOSG_ID")
    private String faceBosgId;
    @TableField("PSN_TMPL_NO")
    private String psnTmplNo;
    @TableField("FACE_FTUR_DATA")
    private byte[] faceFturData;
}