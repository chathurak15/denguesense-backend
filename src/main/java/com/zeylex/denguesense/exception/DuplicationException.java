package com.zeylex.denguesense.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class DuplicationException extends RuntimeException {
    public DuplicationException(String message) {
        super(message);
    }
}
