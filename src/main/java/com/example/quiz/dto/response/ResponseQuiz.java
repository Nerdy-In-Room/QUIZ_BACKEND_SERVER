package com.example.quiz.dto.response;

public record ResponseQuiz(String problem, String correctAnswer, String description, Integer quizCount) {
}
