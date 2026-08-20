package com.businessos.shared.exception;

import org.springframework.http.HttpStatus;

public class InternalServerException extends ApiException {

    public InternalServerException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}