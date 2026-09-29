package com.agrishield.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Getter
public class PermitDeniedException extends RuntimeException {
    private final List<String> lockedTagIds;

    public PermitDeniedException(String message, List<String> lockedTagIds) {
        super(message);
        this.lockedTagIds = lockedTagIds;
    }
}
