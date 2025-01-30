package com.example.quiz.dto.room.request;

public record RoomCreateRequest(String roomName, Long topicId, Integer maxPeople, Integer quizCount, String UUID) {
}
