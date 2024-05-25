package com.example.quiz.dto.room.request;


import com.example.quiz.entity.Room;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomCreateRequest {
    private String roomName;
    private String subject;
    private Integer maxPeople;
    private Integer quizCnt;
    private String masterEmail;
    private boolean removeStatus;

    public RoomCreateRequest(String roomName, String subject, Integer maxPeople, Integer quizCnt, String masterEmail) {
        this.roomName = roomName;
        this.subject = subject;
        this.maxPeople = maxPeople;
        this.quizCnt = quizCnt;
        this.masterEmail = masterEmail;
    }

    public Room toEntity() {
        return Room.builder()
                .roomName(roomName)
                .subject(subject)
                .maxPeople(maxPeople)
                .quizCnt(quizCnt)
                .removeStatus(false)
                .masterEmail(masterEmail)
                .build();
    }
}
