package com.example.quiz.game.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RequestUserInfoAnswer {
    private Long userId;
    private List<Long> questionList;
    private Integer quizCount;
}
