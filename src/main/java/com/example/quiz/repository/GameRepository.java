package com.example.quiz.repository;

import com.example.quiz.entity.Game;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
public interface GameRepository extends MongoRepository<Game, String> {
}
