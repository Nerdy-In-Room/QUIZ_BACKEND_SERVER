package com.example.quiz.exception.general;

import lombok.Getter;

@Getter
public class GeneralErrorException extends RuntimeException {
    private final GeneralErrorCode errorCode;
    private final Object additionalData;

    public GeneralErrorException(GeneralErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.additionalData = null;
    }

    public GeneralErrorException(GeneralErrorCode errorCode, Object additionalData) {
        super(errorCode.getMessage() + " (" + additionalData + ")");
        this.errorCode = errorCode;
        this.additionalData = additionalData;
    }
}
