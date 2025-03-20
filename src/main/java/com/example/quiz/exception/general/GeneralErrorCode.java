package com.example.quiz.exception.general;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GeneralErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "로그인 해주세요.");

    private final HttpStatus status;
    private final String message;
}
