package com.example.quiz.entity;

import java.util.Set;
import javax.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collation = "game")
@Builder
public class Game {

    @Id
    @Column(nullable = false)
    private String id;
    private Integer currentParticipantsNo;
    private Boolean isGaming;
    @Field("gameUser")
    private Set<User> gameUser;
}
