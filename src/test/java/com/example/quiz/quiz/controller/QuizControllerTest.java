package com.example.quiz.quiz.controller;

import com.example.quiz.quiz.dto.request.RequestAnswer;
import com.example.quiz.quiz.service.QuizService;
import com.example.quiz.room.exception.RoomErrorCode;
import com.example.quiz.room.exception.RoomErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    @Mock
    private QuizService quizService;

    @InjectMocks
    private QuizController quizController;

    @Test
    @DisplayName("퀴즈 출제 잘 되는지 확인")
    void startQuiz_withValidRoomId_shouldStartSuccessfully() {
        // given
        String validRoomId = "1";

        // when & then
        assertDoesNotThrow(() -> quizController.startQuiz(validRoomId));
        verify(quizService, times(1)).startQuiz(validRoomId);
    }

    @Test
    @DisplayName("방이 없을때 예외처리가 잘 되는지 확인")
    void startQuiz_withInvalidRoomId_shouldThrowException() {
        // given
        String invalidRoomId = "999";
        doThrow(new RoomErrorException(RoomErrorCode.NOT_FOUND_ROOM, "Room not found"))
                .when(quizService).startQuiz(invalidRoomId);

        // when & then
        RoomErrorException exception = assertThrows(RoomErrorException.class, () -> quizController.startQuiz(invalidRoomId));
        assertEquals(RoomErrorCode.NOT_FOUND_ROOM, exception.getErrorCode());
    }

    @Test
    @DisplayName("정답 체크 잘 되는지 확인")
    void checkQuiz_withCorrectAnswer_shouldProcessSuccessfully() {
        // given
        String roomId = "123";
        RequestAnswer correctAnswer = new RequestAnswer(1L, "정답", false);

        // when & then
        assertDoesNotThrow(() -> quizController.checkQuiz(roomId, correctAnswer));
        verify(quizService, times(1)).checkAnswer(roomId, correctAnswer);
    }

    @Test
    @DisplayName("오답일때 검증")
    void checkQuiz_withIncorrectAnswer_shouldProcessSuccessfully() {
        // given
        String roomId = "123";
        RequestAnswer incorrectAnswer = new RequestAnswer(1L,"오답", false);

        // when & then
        assertDoesNotThrow(() -> quizController.checkQuiz(roomId, incorrectAnswer));
        verify(quizService, times(1)).checkAnswer(roomId, incorrectAnswer);
    }

    @Test
    @DisplayName("정답 체크 할때 방이 없을때")
    void checkQuiz_withInvalidRoomId_shouldThrowException() {
        // given
        String invalidRoomId = "999";
        RequestAnswer answer = new RequestAnswer(1L,"정답", false);
        doThrow(new RoomErrorException(RoomErrorCode.NOT_FOUND_ROOM, "Room not found"))
                .when(quizService).checkAnswer(invalidRoomId, answer);

        // when & then
        RoomErrorException exception = assertThrows(RoomErrorException.class,
                () -> quizController.checkQuiz(invalidRoomId, answer));
        assertEquals(RoomErrorCode.NOT_FOUND_ROOM, exception.getErrorCode());
    }
}