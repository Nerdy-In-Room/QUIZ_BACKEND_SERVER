package com.example.quiz.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
public class GameUser {
    private Long userId;
    private String userRole;
    private Boolean readyStatus;
}

