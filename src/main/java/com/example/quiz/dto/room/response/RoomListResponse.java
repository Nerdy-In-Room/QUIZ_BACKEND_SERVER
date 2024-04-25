package com.example.quiz.dto.room.response;


import com.example.quiz.entity.Room;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomListResponse {
    private Long roomId;
    private String roomName;
    private String subject;
    private Integer maxPeople;
    private Integer quizCnt;
    private Integer currentPeople;

    @Builder
    public RoomListResponse(Room room) {
        this.roomId = room.getRoomId();
        this.roomName = room.getRoomName();
        this.subject = room.getSubject();
        this.maxPeople = room.getMaxPeople();
        this.quizCnt = room.getQuizCnt();
    }
}
