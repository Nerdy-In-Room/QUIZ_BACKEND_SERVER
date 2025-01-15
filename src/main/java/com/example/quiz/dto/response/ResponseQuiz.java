package com.example.quiz.dto.response;

public record ResponseQuiz(Long userId, String email, Long quizId, Integer quizCount) {
}
