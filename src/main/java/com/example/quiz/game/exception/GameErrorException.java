package com.example.quiz.game.exception;

import com.example.quiz.global.exception.CustomErrorException;
import lombok.Getter;

@Getter
public class GameErrorException extends CustomErrorException {
    private final GameErrorCode errorCode;
    private final Object additionalData;

    public GameErrorException(GameErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode);
        this.errorCode = errorCode;
        this.additionalData = null;
    }

    public GameErrorException(GameErrorCode errorCode, Object additionalData) {
        super(errorCode.getMessage(), errorCode);
        this.errorCode = errorCode;
        this.additionalData = additionalData;
    }
}
