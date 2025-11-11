package com.biometric.serv.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 人脸生物特征实体类
 */
@Data
@TableName("bosg_face_ftur_d")
public class BosgFaceFturD implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 人脸特征ID
     */
    @TableId
    private String faceBosgId;

    /**
     * 人员模板号
     */
    private String psnTmplNo;

    /**
     * 证件号码
     */
    private String certno;

    /**
     * 个人证件类型
     */
    private String psnCertType;

    /**
     * 人员姓名
     */
    private String psnName;

    /**
     * 人脸图像URL
     */
    private String faceImgUrl;

    /**
     * 人脸特征数据
     */
    private byte[] faceFturData;

    /**
     * 人脸建模类型
     */
    private String faceCrteTmplType;

    /**
     * 原始照类型
     */
    private String initPhotoType;

    /**
     * 核验模板ID
     */
    private String checkTmplId;

    /**
     * 核验模板类型
     */
    private String checkTmplType;

    /**
     * 算法版本号
     */
    private String algoVerId;

    /**
     * 人脸模板类型
     */
    private String faceTmplType;

    /**
     * 人脸建模方式
     */
    private String faceCrteTmplWay;

    /**
     * 人脸采集方式
     */
    private String faceClctWay;

    /**
     * 人脸采集时间
     */
    private Date faceClctTime;

    /**
     * 人脸采集设备类型
     */
    private String faceClctDevType;

    /**
     * 人脸采集设备编码
     */
    private String faceClctDevCode;

    /**
     * 人脸模板状态
     */
    private String faceTmplStas;

    /**
     * 比对建模分数
     */
    private BigDecimal cprSco;

    /**
     * 比对模板
     */
    private String cprTmplUrl;

    /**
     * 算法升级版本状态
     */
    private String algoRiseVerStas;

    /**
     * 同步状态
     */
    private String syncStas;

    /**
     * 渠道ID
     */
    private String chnlId;

    /**
     * 创建模板凭证
     */
    private String crteTmplCert;

    /**
     * 创建模板理由
     */
    private String crteTmplRea;

    /**
     * 归档ID
     */
    private String archId;

    /**
     * 归档状态
     */
    private String archStas;

    /**
     * 归档消息
     */
    private String archMsg;

    /**
     * 归档人员模板号
     */
    private String archPsnTmplNo;

    /**
     * 归档人员姓名
     */
    private String archPsnName;

    /**
     * 归档证件号码
     */
    private String archCertno;

    /**
     * 未核验重复人员编号
     */
    private String duplPsnNo;

    /**
     * 创建人ID
     */
    private String crterId;

    /**
     * 创建人姓名
     */
    private String crterName;

    /**
     * 创建机构编号
     */
    private String crteOptinsNo;

    /**
     * 人脸采集机构名称
     */
    private String crteOptinsName;

    /**
     * 医保区划
     */
    private String admdvs;

    /**
     * 第三方同步状态
     */
    private String ttpSyncStas;

    /**
     * 第三方同步结果ID
     */
    private String ttpSyncRsltId;

    /**
     * 经办时间
     */
    private Date optTime;

    /**
     * 经办人ID
     */
    private String opterId;

    /**
     * 经办人姓名
     */
    private String opterName;

    /**
     * 经办机构编号
     */
    private String optinsNo;

    /**
     * 有效标志
     */
    private String valiFlag;

    /**
     * 数据唯一记录号
     */
    private String rid;

    /**
     * 数据创建时间
     */
    private Date crteTime;

    /**
     * 数据更新时间
     */
    private Date updtTime;
}

