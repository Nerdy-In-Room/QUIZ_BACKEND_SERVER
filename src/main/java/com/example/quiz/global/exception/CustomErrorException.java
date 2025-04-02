package com.example.quiz.global.exception;

import lombok.Getter;

@Getter
public class CustomErrorException extends RuntimeException {
    private final ErrorCode errorCode;

    public CustomErrorException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
