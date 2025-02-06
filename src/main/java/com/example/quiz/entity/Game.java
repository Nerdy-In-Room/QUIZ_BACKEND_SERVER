package com.example.quiz.entity;

import com.example.quiz.vo.InGameUser;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Game {
    @Id
    @Column(nullable = false)
    private String id;
    private Long roomId;
    private Integer currentParticipantsNo;
    private Boolean isGaming;
    @Field("gameUser")
    private Set<InGameUser> gameUser;

    public void changeGameStatus(boolean status) {
        this.isGaming = status;
    }
    public void changeCurrentParticipantsNo(Integer currentParticipantsNo) { this.currentParticipantsNo = currentParticipantsNo; }
}
