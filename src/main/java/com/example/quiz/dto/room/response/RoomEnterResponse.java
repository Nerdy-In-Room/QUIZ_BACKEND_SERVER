package com.example.quiz.dto.room.response;


import com.example.quiz.entity.Room;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RoomEnterResponse {
    private String roomName;
    private String subject;
    private Integer maxPeople;
    private Integer quizCnt;
    private boolean removeStatus;

    @Builder
    public RoomEnterResponse (Room room) {
        this.roomName = room.getRoomName();
        this.subject = room.getSubject();
        this.maxPeople = room.getMaxPeople();
        this.quizCnt = room.getQuizCnt();
        this.removeStatus = room.isRemoveStatus();
    }
}
