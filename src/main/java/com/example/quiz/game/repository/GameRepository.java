package com.example.quiz.game.repository;

import com.example.quiz.game.entity.Game;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
public interface GameRepository extends MongoRepository<Game, String> {
    void removeById(String id);
}
