package com.example.quiz.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Room implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;
    private String roomName;
    private String subject;
    private Integer maxPeople;
    private Integer quizCnt;
    private String masterEmail;
    @Column(columnDefinition = "TINYINT(0)")
    private boolean removeStatus;

    @Builder
    public Room(String roomName, String subject, Integer maxPeople, Integer quizCnt, boolean removeStatus, String masterEmail) {
        this.roomName = roomName;
        this.subject = subject;
        this.maxPeople = maxPeople;
        this.quizCnt = quizCnt;
        this.removeStatus = removeStatus;
        this.masterEmail = masterEmail;
    }

    public void changeRemoveStatus() {
        this.removeStatus = false;
    }

    public void changeRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void changeSubject(String subject) {
        this.subject = subject;
    }
}
