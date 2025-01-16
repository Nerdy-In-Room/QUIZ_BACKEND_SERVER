package com.example.quiz.entity;

import jakarta.persistence.*;
import lombok.*;
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
        this.removeStatus = true;
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
