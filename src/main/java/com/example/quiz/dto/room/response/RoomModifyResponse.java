package com.example.quiz.dto.room.response;


import com.example.quiz.entity.Room;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomModifyResponse {
    private String roomName;
    private Long topicId;

    @Builder
    public RoomModifyResponse(Room room) {
        this.roomName = room.getRoomName();
        this.topicId = room.getTopicId();
    }
}
