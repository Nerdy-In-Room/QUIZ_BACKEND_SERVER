package com.example.quiz.room.dto.response;

import com.example.quiz.global.type.Role;

public record QuizRoomEnterResponse(Long userId, String email, Long quizId, Integer quizCount, Role role) {
}
