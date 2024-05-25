package com.example.quiz.dto.room.request;


import com.example.quiz.entity.Room;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomCreateRequest {
    private String roomName;
    private Long topicId;
    private Integer maxPeople;
    private Integer quizCnt;
    private String masterEmail;
    private boolean removeStatus;

    public RoomCreateRequest(String roomName, Long topicId, Integer maxPeople, Integer quizCnt, String masterEmail) {
        this.roomName = roomName;
        this.topicId = topicId;
        this.maxPeople = maxPeople;
        this.quizCnt = quizCnt;
        this.masterEmail = masterEmail;
    }

    public Room toEntity() {
        return Room.builder()
                .roomName(roomName)
                .topicId(topicId)
                .maxPeople(maxPeople)
                .quizCount(quizCnt)
                .removeStatus(false)
                .masterEmail(masterEmail)
                .build();
    }
}
