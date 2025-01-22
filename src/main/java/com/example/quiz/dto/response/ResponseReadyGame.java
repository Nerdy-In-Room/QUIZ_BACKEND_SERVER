package com.example.quiz.dto.response;

import com.example.quiz.enums.Role;

public record ResponseReadyGame(Long userId, String email, Role role, boolean readyStatus, boolean allReadyStatus) {
}
