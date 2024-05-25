package com.example.quiz.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestUserId {
    // 유저의 상태가 필요하고, 유저의
    //private MessageType messageType;
    private Long userId;
}
