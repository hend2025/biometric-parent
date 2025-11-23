package com.biometric.serv.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@TableName("bosg_psn_tmpl_d")
public class PsnTmpl implements Serializable {

    @TableId
    private String psnTmplNo;

    private String psnName;

    private String certno;

    private LocalDate brdy;

    private String gend;
}
