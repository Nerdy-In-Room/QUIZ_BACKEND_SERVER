package com.example.quiz.dto.room.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoomResponse(Long roomId, String roomName, Long topicId, Integer maxPeople,
                           Integer quizCount, Integer currentPeople) {
}
