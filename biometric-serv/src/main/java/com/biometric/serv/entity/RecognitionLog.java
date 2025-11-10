package com.biometric.serv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 识别日志实体类
 */
@Data
@TableName("tb_recognition_log")
public class RecognitionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（识别成功时记录）
     */
    private Long userId;

    /**
     * 人脸ID（识别成功时记录）
     */
    private String faceId;

    /**
     * 相似度分数
     */
    private Double similarity;

    /**
     * 识别类型：1-1:N识别，2-1:1验证
     */
    private Integer recognitionType;

    /**
     * 识别结果：0-失败，1-成功
     */
    private Integer recognitionResult;

    /**
     * 查询图片URL
     */
    private String queryImageUrl;

    /**
     * 耗时（毫秒）
     */
    private Long costTime;

    /**
     * 创建时间
     */
    private Date createTime;
}

