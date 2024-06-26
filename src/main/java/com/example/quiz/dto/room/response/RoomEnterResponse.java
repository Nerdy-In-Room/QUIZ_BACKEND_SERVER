package com.example.quiz.dto.room.response;


import com.example.quiz.entity.Room;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RoomEnterResponse {
    private Long roomId;
    private String roomName;
    private Long topicId;
    private Integer maxPeople;
    private Integer quizCnt;
    private boolean removeStatus;

    @Builder
    public RoomEnterResponse (Room room) {
        this.roomId = room.getRoomId();
        this.roomName = room.getRoomName();
        this.topicId = room.getTopicId();
        this.maxPeople = room.getMaxPeople();
        this.quizCnt = room.getQuizCount();
        this.removeStatus = room.getRemoveStatus();
    }
}
