package com.example.quiz.room.dto.response;

import com.example.quiz.global.type.Role;
import com.example.quiz.game.model.InGameUser;

import java.util.Set;

public record RoomEnterResponse (Long roomId, String roomName, Long topicId, Integer maxPeople, Integer quizCount, boolean removeStatus, Role role,
        InGameUser inGameUser, Set<InGameUser> participants) {
}
