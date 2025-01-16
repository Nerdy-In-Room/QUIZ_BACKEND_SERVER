package com.example.quiz.controller;

import com.example.quiz.dto.request.RequestAnswer;
import com.example.quiz.dto.request.RequestUserInfoAnswer;
import com.example.quiz.dto.response.ResponseQuiz;
import com.example.quiz.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class QuizController {
    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;

    @MessageMapping("/{id}/send")
    public void sendQuiz(@DestinationVariable String id, RequestUserInfoAnswer userInfoAnswer){
        ResponseQuiz responseQuiz = gameService.sendQuiz(id,userInfoAnswer);
        log.info(responseQuiz.toString());
        messagingTemplate.convertAndSend("/pub/quiz/"+id, responseQuiz);
    }

    @MessageMapping("/{id}/check")
    public void checkQuiz(@DestinationVariable String id, RequestAnswer requestAnswer){
        log.info("응답");
        ResponseQuiz responseQuiz = gameService.checkAnswer(id, requestAnswer);

        messagingTemplate.convertAndSend("/pub/quiz"+id,responseQuiz);
    }
}
