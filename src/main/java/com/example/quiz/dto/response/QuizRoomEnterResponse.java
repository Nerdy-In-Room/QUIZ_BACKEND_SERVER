package com.example.quiz.dto.response;

import com.example.quiz.enums.Role;

public record QuizRoomEnterResponse(Long userId, String email, Long quizId, Integer quizCount, Role role) {
}
