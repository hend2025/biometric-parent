package com.biometric.algo.exception;

/**
 * 算法服务异常基础类
 * 所有算法相关异常的父类，支持错误码和异常链
 * 
 * @author biometric-algo
 * @version 1.0
 */
public class AlgoException extends RuntimeException {
    
    /** 错误码（-1表示默认错误） */
    private final int errorCode;
    
    /**
     * 构造函数
     * 
     * @param message 错误信息
     */
    public AlgoException(String message) {
        super(message);
        this.errorCode = -1;
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误信息
     * @param cause 原始异常
     */
    public AlgoException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = -1;
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public AlgoException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误信息
     * @param cause 原始异常
     */
    public AlgoException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public int getErrorCode() {
        return errorCode;
    }
}
