package com.example.quiz.user.dto.request;

import com.example.quiz.global.type.Role;

public record LoginUserRequest(Long userId, String email, Role role) {
}
