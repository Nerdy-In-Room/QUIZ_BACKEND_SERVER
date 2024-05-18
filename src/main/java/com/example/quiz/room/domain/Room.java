package com.example.quiz.room.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private Long topicId;

    private String roomName;

    private Integer maxPeople;

    private Integer quizCount;

    @ColumnDefault("false")
    @Column(columnDefinition = "TINYINT(1)")
    private Boolean removeStatus;

    private String masterEmail;

}
