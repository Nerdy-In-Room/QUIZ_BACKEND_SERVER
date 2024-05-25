package com.example.quiz.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class Room implements Serializable {

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

    public void changeRemoveStatus() {
        this.removeStatus = false;
    }

    public void changeRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void changeSubject(Long topicId) {
        this.topicId = topicId;
    }
}
