package com.example.quiz.quiz.dto.response;

import com.example.quiz.global.type.Role;

public record ResponseReadyGame(Long userId, String email, Role role, boolean readyStatus, boolean allReadyStatus) {
}
