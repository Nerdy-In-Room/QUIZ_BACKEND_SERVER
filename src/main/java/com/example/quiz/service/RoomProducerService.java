package com.example.quiz.service;

import com.example.quiz.config.websocket.WebSocketEventListener;
import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.response.RoomListResponse;
import com.example.quiz.entity.Game;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.User;
import com.example.quiz.enums.Role;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.RoomRepository;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class RoomProducerService {
    private static final int PAGE_SIZE = 10;
    private static final Logger log = LoggerFactory.getLogger(RoomProducerService.class);

    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;
    private final WebSocketEventListener webSocketEventListener;

    public long createRoom(RoomCreateRequest roomRequest) {
        Room room = roomRequest.toEntity();
        long roomId = roomRepository.save(room).getRoomId();

        User user = new User(1L, Role.ADMIN, false);

        Game game = gameRepository.save(
                new Game(String.valueOf(roomId), 1, false, new HashSet<>()));

        game.getGameUser().add(user);
        gameRepository.save(game);

        // kafkaTemplate.send(TOPIC, roomId, room);

        return roomId;
    }

    public Page<RoomListResponse> roomList(int index) {
        Pageable pageable = PageRequest.of(index, PAGE_SIZE, Sort.by("roomId").descending());

        return roomRepository.findAllByRemoveStatus(false, pageable)
                .map(room -> {
                    RoomListResponse roomListResponse = new RoomListResponse(room);
                    roomListResponse.setCurrentPeople(webSocketEventListener.getSubscriptionCount(room.getRoomId()));

                    return roomListResponse;
                });
    }
}