package com.biometric.algo.exception;

/**
 * Exception for algorithm processing errors
 */
public class AlgoProcessException extends AlgoException {
    
    public AlgoProcessException(int errorCode, String message) {
        super(errorCode, message);
    }
    
    public AlgoProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
