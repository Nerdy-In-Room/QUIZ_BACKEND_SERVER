package com.example.quiz.exception.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GameErrorCode {
    ALREADY_IN_ANOTHER_ROOM(HttpStatus.BAD_REQUEST, "이미 다른방에 입장중입니다."),
    MAX_ROOM(HttpStatus.BAD_REQUEST, "방이 가득 찼습니다"),
    USER_NOT_READY(HttpStatus.BAD_REQUEST, "모든 유저가 준비하지 않았습니다"),
    NOT_FOUND_ROOM(HttpStatus.BAD_REQUEST, "존재하지 않는 방입니다"),
    USER_NOT_IN_GAME(HttpStatus.BAD_REQUEST, "게임에 접속중이지 않습니다."),
    QUIZ_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "퀴즈를 찾을 수 없습니다.");


    private final HttpStatus status;
    private final String message;
}
