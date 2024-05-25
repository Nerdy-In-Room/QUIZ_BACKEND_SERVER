package com.example.quiz.game.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseMessage {
    private Long userId;
    private boolean isCheckedAnswer;
}
