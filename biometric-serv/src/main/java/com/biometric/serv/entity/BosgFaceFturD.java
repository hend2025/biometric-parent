package com.biometric.serv.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("bosg_face_ftur_d")
public class BosgFaceFturD implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String faceBosgId;

    private String psnTmplNo;

    private String certno;

    private String psnCertType;

    private String psnName;

    private String faceImgUrl;

    private byte[] faceFturData;

    private String algoVerId;

    private String valiFlag;

}
