package com.example.quiz.exception;

import com.example.quiz.exception.game.GameErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ServiceExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ServiceExceptionHandler.class);

    @ExceptionHandler(GameErrorException.class)
    public ResponseEntity<Map<String, Object>> handleCustomErrorException(GameErrorException ex) {
        log.error("CustomErrorException 발생: {}, 상태 코드: {}", ex.getMessage(), ex.getErrorCode().getStatus());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", ex.getErrorCode().getStatus().value());
        errorResponse.put("error", ex.getErrorCode().name());
        errorResponse.put("message", ex.getMessage());

        return new ResponseEntity<>(errorResponse, ex.getErrorCode().getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Exception 발생: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 500);
        errorResponse.put("error", "INTERNAL_SERVER_ERROR");
        errorResponse.put("message", "서버 내부 오류가 발생했습니다.");

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
