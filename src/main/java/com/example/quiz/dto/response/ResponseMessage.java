package com.example.quiz.dto.response;

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
