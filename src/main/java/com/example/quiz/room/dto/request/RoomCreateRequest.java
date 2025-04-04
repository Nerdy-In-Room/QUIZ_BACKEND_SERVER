package com.example.quiz.room.dto.request;

public record RoomCreateRequest(String roomName, Long topicId, Integer maxPeople, Integer quizCount, String UUID) {
}
