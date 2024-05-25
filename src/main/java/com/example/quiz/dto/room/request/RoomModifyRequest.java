package com.example.quiz.dto.room.request;


import com.example.quiz.entity.Room;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomModifyRequest {
    private String roomName;
    private String subject;

    public RoomModifyRequest(String roomName, String subject) {
        this.roomName = roomName;
        this.subject = subject;
    }

    public Room toEntity() {
        return Room.builder()
                .roomName(roomName)
                .subject(subject)
                .build();
    }
}
