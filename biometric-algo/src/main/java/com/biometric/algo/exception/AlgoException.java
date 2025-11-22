package com.biometric.algo.exception;

/**
 * Base exception for algorithm service errors
 */
public class AlgoException extends RuntimeException {
    
    private final int errorCode;
    
    public AlgoException(String message) {
        super(message);
        this.errorCode = -1;
    }
    
    public AlgoException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = -1;
    }
    
    public AlgoException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AlgoException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
}
