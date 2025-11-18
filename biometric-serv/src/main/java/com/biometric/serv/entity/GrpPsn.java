package com.biometric.serv.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("bosg_cpr_grp_psn_b")
public class GrpPsn implements Serializable {
    @TableId("GRP_PSN_ID")
    private String grpPsnId;
    @TableField("GRP_ID")
    private String grpId;
    @TableField("PSN_TMPL_NO")
    private String psnTmplNo;
}