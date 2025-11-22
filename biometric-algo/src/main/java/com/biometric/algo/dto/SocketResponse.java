package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.biometric.algo.dto.deserializer.StringToObjectDeserializer;
import lombok.Data;

/**
 * Socket响应基础类
 * 所有算法接口响应的通用结构，包含返回码、描述和数据
 * 
 * 响应结构：
 * - RETURNID: 返回码（0表示成功，非0表示失败）
 * - RETURNDESC: 返回描述信息
 * - RETURNVALUE: 返回数据（泛型，根据具体接口而定）
 * 
 * @param <T> 返回数据的类型
 * @author biometric-algo
 * @version 1.0
 */
@Data
public class SocketResponse<T> {

    /** 返回码（0=成功，非0=失败） */
    @JSONField(name = "RETURNID")
    private int returnId;

    /** 返回描述信息 */
    @JSONField(name = "RETURNDESC")
    private String returnDesc;

    /** 返回数据（使用自定义反序列化器处理字符串转对象） */
    @JSONField(name = "RETURNVALUE", deserializeUsing = StringToObjectDeserializer.class)
    private T returnValue;

}