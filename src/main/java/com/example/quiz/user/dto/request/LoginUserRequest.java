package com.example.quiz.user.dto.request;

public record LoginUserRequest(Long userId, String email, String role) {
}
