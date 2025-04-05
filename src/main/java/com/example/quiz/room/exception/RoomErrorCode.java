package com.example.quiz.room.exception;

import com.example.quiz.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RoomErrorCode implements ErrorCode {
    ALREADY_IN_ANOTHER_ROOM(HttpStatus.BAD_REQUEST, "이미 다른방에 입장중입니다."),
    MAX_ROOM(HttpStatus.BAD_REQUEST, "정원이 초과되었습니다."),
    NOT_FOUND_ROOM(HttpStatus.BAD_REQUEST, "존재하지 않는 방입니다."),
    WRONG_MAX_PEOPLE(HttpStatus.BAD_REQUEST, "최대인원은 현재인원보다 작을 수 없습니다."),
    FAIL_MODIFY_ROOM(HttpStatus.FORBIDDEN, "방 수정에 실패했습니다."),
    FAIL_ENTER_ROOM(HttpStatus.INTERNAL_SERVER_ERROR, "방 입장에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
