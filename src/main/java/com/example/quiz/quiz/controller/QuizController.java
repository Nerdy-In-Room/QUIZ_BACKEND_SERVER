package com.example.quiz.quiz.controller;

import com.example.quiz.game.service.GameService;
import com.example.quiz.global.config.auth.annotation.user.LoginUser;
import com.example.quiz.quiz.dto.request.RequestAnswer;
import com.example.quiz.quiz.service.QuizService;
import com.example.quiz.room.dto.response.QuizRoomEnterResponse;
import com.example.quiz.user.dto.request.LoginUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
public class QuizController {
    private final QuizService quizService;

    @MessageMapping("/{id}/send")
    public void sendQuiz(@DestinationVariable String id){
        quizService.sendQuiz(id);
    }

    @MessageMapping("/{id}/check")
    public void checkQuiz(@DestinationVariable String id, RequestAnswer requestAnswer){
        quizService.checkAnswer(id, requestAnswer);
    }

    @GetMapping("/quiz/{roomId}")
    public ModelAndView enterGameRoom(@PathVariable Long roomId, @LoginUser LoginUserRequest loginUserRequest) {
        QuizRoomEnterResponse quizRoomEnterResponse = quizService.enterGameRoom(roomId, loginUserRequest);
        Map<String, Object> map = new HashMap<>();
        map.put("responseQuiz", quizRoomEnterResponse);

        return new ModelAndView("quiz", map);
    }
}
