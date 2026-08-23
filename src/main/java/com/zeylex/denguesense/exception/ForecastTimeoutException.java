package com.zeylex.denguesense.exception;

public class ForecastTimeoutException extends RuntimeException {
    public ForecastTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
