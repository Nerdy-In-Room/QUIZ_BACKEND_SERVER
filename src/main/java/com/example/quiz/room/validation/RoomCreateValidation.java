package com.example.quiz.room.validation;

import com.example.quiz.global.exception.general.GeneralErrorCode;
import com.example.quiz.global.exception.general.GeneralErrorException;

public class RoomCreateValidation {
    public static void validateRoomName(String roomName) {
        if (roomName == null || roomName.isEmpty() || roomName.length() > 20) {
            throw new GeneralErrorException(GeneralErrorCode.INVALID_INPUT, "방 제목의 최대 길이는 20자 입니다.");
        }
    }

    public static void validateMaxPeople(int maxPeople) {
        if (maxPeople < 1 || maxPeople > 8) {
            throw new GeneralErrorException(GeneralErrorCode.INVALID_INPUT, "최대 인원은 8명입니다.");
        }
    }

    public static void validateQuizCount(int quizCount) {
        if (quizCount < 1 || quizCount > 10) {
            throw new GeneralErrorException(GeneralErrorCode.INVALID_INPUT, "최대 문제수는 10문제입니다.");
        }
    }
}
