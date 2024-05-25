package com.example.quiz.controller;

import com.example.quiz.dto.request.RequestUserId;
import com.example.quiz.dto.response.ResponseMessage;
import com.example.quiz.service.GameService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class GameController {

    // 메세지 템플릿을 사용하여 데이터를 전달
    // 이유는 게임의 상태를 정의 할때와 게임의 시작할때 전송하는 데이터의 구조가 변경이 된므로 템플릿사용
    private SimpMessagingTemplate messagingTemplate;

    private final GameService gameService;

    @MessageMapping("/{id}")
    public void enter(@DestinationVariable String id, RequestUserId requestUserId){
        ResponseMessage responseMessage = gameService.gameStatusService(id, requestUserId.getUserId());
        messagingTemplate.convertAndSend("/pub/"+id,responseMessage);
    }
}
