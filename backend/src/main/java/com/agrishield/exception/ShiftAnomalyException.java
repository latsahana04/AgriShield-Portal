package com.agrishield.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ShiftAnomalyException extends RuntimeException {
    public ShiftAnomalyException(String message) {
        super(message);
    }
}
