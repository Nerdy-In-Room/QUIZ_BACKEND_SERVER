package com.example.quiz.service;

import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.response.RoomEnterResponse;
import com.example.quiz.dto.room.response.RoomListResponse;
import com.example.quiz.entity.Game;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.User;
import com.example.quiz.enums.Role;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.RoomRepository;
import java.util.ArrayList;
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

    public RoomEnterResponse createRoom(RoomCreateRequest roomRequest) {
        Room room = roomRequest.toEntity();
        long roomId = roomRepository.save(room).getRoomId();

        User user = new User(1L, Role.ADMIN, false);

        Game game = gameRepository.save(
        new Game(String.valueOf(roomId), 1, false, new HashSet<>()));

        game.getGameUser().add(user);
        gameRepository.save(game);

        // kafkaTemplate.send(TOPIC, roomId, room);

        return new RoomEnterResponse(room);
    }

    public Page<RoomListResponse> roomList(int index) {
        Pageable pageable = PageRequest.of(index, PAGE_SIZE, Sort.by("roomId").descending());

        Page<RoomListResponse> roomListResponsePage = roomRepository.findAllByRemoveStatus(false, pageable)
                .map(room -> {
                    RoomListResponse roomListResponse = new RoomListResponse(room);
                    Game game = gameRepository.findById(String.valueOf(room.getRoomId())).orElseThrow();
                    roomListResponse.setCurrentPeople(game.getGameUser().size());
                    return roomListResponse;
                });

        if (roomListResponsePage.isEmpty()) {
            // simpMessagingTemplate.convertAndSend("/room", "[]");
            log.info("list empty");
            return null;
        }

        return roomListResponsePage;
    }
}