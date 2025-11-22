package com.biometric.algo.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.biometric.algo.dto.deserializer.StringToObjectDeserializer;
import lombok.Data;

@Data
public class SocketResponse<T> {

    @JSONField(name = "RETURNID")
    private int returnId;

    @JSONField(name = "RETURNDESC")
    private String returnDesc;

    @JSONField(name = "RETURNVALUE", deserializeUsing = StringToObjectDeserializer.class)
    private T returnValue;

}