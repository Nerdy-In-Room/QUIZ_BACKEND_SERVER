package com.example.quiz.room.repository;


import com.example.quiz.room.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz,Long> {
    List<Quiz> findAllByTopicId(Long topicId);
}
