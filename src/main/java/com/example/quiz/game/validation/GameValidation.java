package com.example.quiz.game.validation;

import com.example.quiz.game.exception.GameErrorCode;
import com.example.quiz.game.exception.GameErrorException;

public class GameValidation {
    public static void validateAnswer(String answer) {
        if (answer == null || answer.isEmpty()) {
            throw new GameErrorException(GameErrorCode.EMPTY_ANSWER, "정답을 입력해주세요.");
        }
    }
}
