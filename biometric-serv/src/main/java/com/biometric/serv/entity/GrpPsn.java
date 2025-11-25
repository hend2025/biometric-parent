package com.biometric.serv.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("bosg_cpr_grp_psn_b")
public class GrpPsn implements Serializable {
    @TableId
    private String grpPsnId;
    private String psnTmplNo;
    private String grpId;
}