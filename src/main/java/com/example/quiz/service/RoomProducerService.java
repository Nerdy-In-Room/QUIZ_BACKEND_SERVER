package com.example.quiz.service;

import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.entity.Game;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.User;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.RoomRepository;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class RoomProducerService {
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;

    public long createRoom(RoomCreateRequest roomRequest) {
        Room room = roomRequest.toEntity();
        long roomId = roomRepository.save(room).getRoomId();

        User user = new User(1L, "USER", 0);

        Game game = gameRepository.save(
                new Game(String.valueOf(roomId), 1, false, new ArrayList<>()));
        game.getGameUser().add(user);
        gameRepository.save(game);

//        kafkaTemplate.send(TOPIC, roomId, room);

        return roomId;
    }
}
