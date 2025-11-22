package com.biometric.algo.exception;

/**
 * Exception for socket connection errors
 */
public class SocketConnectionException extends AlgoException {
    
    public SocketConnectionException(String message) {
        super(message);
    }
    
    public SocketConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
