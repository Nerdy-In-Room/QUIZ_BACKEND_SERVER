package com.example.quiz.quiz.controller;

import com.example.quiz.game.service.GameService;
import com.example.quiz.quiz.dto.request.RequestAnswer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class QuizController {
    private final GameService gameService;

    @MessageMapping("/{id}/send")
    public void sendQuiz(@DestinationVariable String id){
        gameService.sendQuiz(id);
    }

    @MessageMapping("/{id}/check")
    public void checkQuiz(@DestinationVariable String id, RequestAnswer requestAnswer){
        gameService.checkAnswer(id, requestAnswer);
    }
}
