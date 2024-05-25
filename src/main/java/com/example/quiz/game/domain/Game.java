package com.example.quiz.game.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Column;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "game")
@Builder
public class Game {

    @Id
    @Column(nullable = false)
    private String id;
    private Integer currentParticipantsNo;
    private Boolean isGaming;
    private List<GameUser> gameUser;
}
