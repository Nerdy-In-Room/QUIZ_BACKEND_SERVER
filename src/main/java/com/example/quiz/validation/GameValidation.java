package com.example.quiz.validation;

import com.example.quiz.exception.general.GeneralErrorCode;
import com.example.quiz.exception.general.GeneralErrorException;

public class GameValidation {
    public static void validateAnswer(String answer) {
        if (answer == null || answer.isEmpty()) {
            throw new GeneralErrorException(GeneralErrorCode.INVALID_INPUT, "정답을 입력해주세요.");
        }
    }
}
