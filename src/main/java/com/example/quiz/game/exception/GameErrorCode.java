package com.example.quiz.game.exception;

import com.example.quiz.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GameErrorCode implements ErrorCode {
    USER_NOT_READY(HttpStatus.BAD_REQUEST, "모든 유저가 준비하지 않았습니다"),
    USER_NOT_IN_GAME(HttpStatus.BAD_REQUEST, "게임에 접속중이지 않습니다."),
    QUIZ_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "퀴즈를 찾을 수 없습니다."),
    EMPTY_ANSWER(HttpStatus.BAD_REQUEST, "정답을 입력해주세요.");

    private final HttpStatus status;
    private final String message;
}
