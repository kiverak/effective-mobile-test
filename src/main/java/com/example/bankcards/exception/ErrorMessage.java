package com.example.bankcards.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ErrorMessage extends RuntimeException {
    private HttpStatus httpStatus;

    public ErrorMessage(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
