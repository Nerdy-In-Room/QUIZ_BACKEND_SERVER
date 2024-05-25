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
    private Long topicId;
    private Integer maxPeople;
    private Integer quizCnt;
    private Integer currentPeople;

    @Builder
    public RoomListResponse(Room room) {
        this.roomId = room.getRoomId();
        this.roomName = room.getRoomName();
        this.topicId = room.getTopicId();
        this.maxPeople = room.getMaxPeople();
        this.quizCnt = room.getQuizCount();
    }
}
