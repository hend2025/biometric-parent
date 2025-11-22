package com.biometric.algo.exception;

/**
 * Socket连接异常
 * 当与算法引擎的Socket连接失败或通信异常时抛出
 * 
 * @author biometric-algo
 * @version 1.0
 */
public class SocketConnectionException extends AlgoException {
    
    /**
     * 构造函数
     * 
     * @param message 错误信息
     */
    public SocketConnectionException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误信息
     * @param cause 原始异常
     */
    public SocketConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
