package com.example.quiz.game.dto.game.request;

import java.util.List;

public record RequestUserInfoAnswer(Long userId, List<Long> questionList, Integer quizCount) {
}
