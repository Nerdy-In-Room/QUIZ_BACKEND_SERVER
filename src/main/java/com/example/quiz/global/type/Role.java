package com.example.quiz.global.type;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Role {
    USER("User"),
    ADMIN("Admin");

    private final String role;
}
