package com.example.quiz.quiz.repository;

import com.example.quiz.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz,Long> {
    List<Quiz> findAllByTopicId(Long topicId);
}
