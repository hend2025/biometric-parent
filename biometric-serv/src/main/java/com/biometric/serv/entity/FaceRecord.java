package com.biometric.serv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 人脸记录实体类
 */
@Data
@TableName("tb_face_record")
public class FaceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 人脸ID（对应算法服务中的faceId）
     */
    private String faceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 特征向量（JSON格式存储）
     */
    private String featureVector;

    /**
     * 状态：0-无效，1-有效
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}

