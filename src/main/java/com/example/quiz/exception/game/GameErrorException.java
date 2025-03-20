package com.example.quiz.exception.game;

import lombok.Getter;

@Getter
public class GameErrorException extends RuntimeException {
    private final GameErrorCode errorCode;
    private final Object additionalData;

    public GameErrorException(GameErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.additionalData = null;
    }

    public GameErrorException(GameErrorCode errorCode, Object additionalData) {
        super(errorCode.getMessage() + " (" + additionalData + ")");
        this.errorCode = errorCode;
        this.additionalData = additionalData;
    }
}
