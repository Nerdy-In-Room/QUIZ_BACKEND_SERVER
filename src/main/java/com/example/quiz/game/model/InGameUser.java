package com.example.quiz.game.model;

import com.example.quiz.global.type.Role;
import lombok.Getter;

@Getter
public class InGameUser {
    private final Long id;
    private final Long roomId;
    private final String username;
    private final Role role;
    private boolean isReadyStatus;

    public InGameUser(Long id, Long roomId, String username, Role role, boolean isReadyStatus) {
        this.id = id;
        this.roomId = roomId;
        this.username = username;
        this.role = role;
        this.isReadyStatus = isReadyStatus;
    }

    public void changeReadyStatus(boolean readyStatus) {
        this.isReadyStatus = readyStatus;
    }
}
