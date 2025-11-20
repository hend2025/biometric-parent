package com.biometric.algo.dto;

import lombok.Data;

@Data
public class SocketResponse<T> {

    private int returnId;

    private String returnDesc;

    private T returnValue;

}