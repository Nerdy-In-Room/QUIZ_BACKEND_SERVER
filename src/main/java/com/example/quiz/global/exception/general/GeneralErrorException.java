package com.example.quiz.global.exception.general;

import com.example.quiz.global.exception.CustomErrorException;
import lombok.Getter;

@Getter
public class GeneralErrorException extends CustomErrorException {
    private final GeneralErrorCode errorCode;
    private final Object additionalData;

    public GeneralErrorException(GeneralErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode);
        this.errorCode = errorCode;
        this.additionalData = null;
    }

    public GeneralErrorException(GeneralErrorCode errorCode, Object additionalData) {
        super(errorCode.getMessage() + " " + additionalData, errorCode);
        this.errorCode = errorCode;
        this.additionalData = additionalData;
    }
}
