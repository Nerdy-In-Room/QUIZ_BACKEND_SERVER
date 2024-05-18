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
        // 방에 들어오면 성공적으로 방에 들어왔다는 알림을 보내야함

        /*if(!gameService.startGame(id)){
            ResponseMessage responseMessage = gameService.gameStatusService(id, requestDto.getUserId());
            messagingTemplate.convertAndSend("/pub/"+id,responseMessage);
            if(gameService.startGame(id)){
                // 방에 문제를 뿌려주는 코드
                ResponseQuize responseQuize = gameService.sendQuiz(id);
                messagingTemplate.convertAndSend("/pub/"+id,responseQuize);
            }

        }else{

            gameService.checkAnswer(id,requestDto);

        }

        *//*log.info("id의 값은 {}",id);
        if (requestDto.getMessageType().equals(MessageType.GAME_STATUS)){
            gameService.gameStatusService(id, requestDto.getUserId());
        }*//*
        return requestDto;*/
        ResponseMessage responseMessage = gameService.gameStatusService(id, requestUserId.getUserId());
        messagingTemplate.convertAndSend("/pub/"+id,responseMessage);
    }
}
