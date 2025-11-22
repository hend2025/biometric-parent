package com.biometric.algo.exception;

/**
 * 算法处理异常
 * 当算法处理过程中发生错误时抛出，如解析响应失败、数据格式错误等
 * 
 * @author biometric-algo
 * @version 1.0
 */
public class AlgoProcessException extends AlgoException {
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public AlgoProcessException(int errorCode, String message) {
        super(errorCode, message);
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误信息
     * @param cause 原始异常
     */
    public AlgoProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
