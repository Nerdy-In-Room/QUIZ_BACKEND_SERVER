package com.example.quiz.dto.room.request;


import com.example.quiz.entity.Room;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomModifyRequest {
    private String roomName;
    private Long topicId;

    public RoomModifyRequest(String roomName, Long topicId) {
        this.roomName = roomName;
        this.topicId = topicId;
    }

    public Room toEntity() {
        return Room.builder()
                .roomName(roomName)
                .topicId(topicId)
                .build();
    }
}
