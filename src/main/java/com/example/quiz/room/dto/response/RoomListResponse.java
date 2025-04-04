package com.example.quiz.room.dto.response;

public record RoomListResponse (Long roomId, String roomName, Long topicId, Integer maxPeople, Integer quizCount, Integer currentPeople) {
}
