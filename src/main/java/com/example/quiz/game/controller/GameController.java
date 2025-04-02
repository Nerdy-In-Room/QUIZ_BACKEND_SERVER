package com.example.quiz.game.controller;

import com.example.quiz.quiz.dto.request.RequestRemainQuiz;
import com.example.quiz.game.dto.request.RequestUserId;
import com.example.quiz.game.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class GameController {
    private final GameService gameService;

    @MessageMapping("/{id}/ready")
    public void ready(@DestinationVariable String id, RequestUserId requestUserId) {
        gameService.toggleReadyStatus(id, requestUserId.userId());
    }

    @MessageMapping("/{id}/start")
    public void start(@DestinationVariable String id, RequestRemainQuiz requestRemainQuiz) {
        gameService.startGame(id, requestRemainQuiz);
    }
}
