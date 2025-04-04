package com.example.quiz.room.exception;

import com.example.quiz.global.exception.CustomErrorException;
import lombok.Getter;

@Getter
public class RoomErrorException extends CustomErrorException {
    private final RoomErrorCode errorCode;
    private final Object additionalData;

    public RoomErrorException(RoomErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode);
        this.errorCode = errorCode;
        this.additionalData = null;
    }

    public RoomErrorException(RoomErrorCode errorCode, Object additionalData) {
        super(errorCode.getMessage() + " (" + additionalData + ")", errorCode);
        this.errorCode = errorCode;
        this.additionalData = additionalData;
    }
}
