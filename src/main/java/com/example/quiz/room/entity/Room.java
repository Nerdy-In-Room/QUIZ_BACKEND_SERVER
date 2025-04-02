package com.example.quiz.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;
    private Long topicId;
    private String roomName;
    private Integer maxPeople;
    private Integer quizCount;
    @ColumnDefault("false")
    @Column(columnDefinition = "TINYINT(1)")
    private Boolean removeStatus;
    private String masterEmail;

    public void removeStatus() {
        this.removeStatus = !removeStatus;
    }

    public void changeRoomName(String roomName) {
        if (roomName != null) {
            this.roomName = roomName;
        }
    }

    public void changeSubject(Long topicId) {
        if (topicId != null) {
            this.topicId = topicId;
        }
    }

    public void changeQuizCount(Integer quizCount) {
        if(quizCount != null) {
            this.quizCount = quizCount;
        }
    }
}
