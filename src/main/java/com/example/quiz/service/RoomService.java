package com.example.quiz.service;

import com.example.quiz.config.websocket.WebSocketEventListener;
import com.example.quiz.dto.response.CurrentOccupancy;
import com.example.quiz.dto.room.request.RoomModifyRequest;
import com.example.quiz.dto.room.response.RoomEnterResponse;
import com.example.quiz.dto.room.response.RoomModifyResponse;
import com.example.quiz.entity.Game;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.User;
import com.example.quiz.enums.Role;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.RoomRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;
    private final WebSocketEventListener webSocketEventListener;

    public RoomEnterResponse enterRoom(long roomId) throws IllegalAccessException {
        Room room = roomRepository.findById(roomId).orElseThrow(IllegalArgumentException::new);

        // user는 jwt에서 가져올 예정
        User user = new User(5L, "user", "sample@sampe.co", Role.USER, false);

        if (user == null) {
            throw new IllegalAccessException();
        }

        Game game = gameRepository.findById(String.valueOf(roomId)).orElseThrow();

        if (room.getMaxPeople() <= game.getGameUser().size()) {
            throw new IllegalArgumentException();
        }

        game.getGameUser().add(user);
        gameRepository.save(game);

        return new RoomEnterResponse(room);
    }

    @Transactional
    public RoomModifyResponse modifyRoom(RoomModifyRequest request, long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(IllegalArgumentException::new);

        if (request.getRoomName() != null) {
            room.changeRoomName(request.getRoomName());
        }

        if (request.getTopicId() != null) {
            room.changeSubject(request.getTopicId());
        }

        return new RoomModifyResponse(room);
    }

    public List<CurrentOccupancy> getCurrentOccupancy(List<Long> currentPageRooms) {

        return currentPageRooms.stream()
                .map(id -> new CurrentOccupancy(id, webSocketEventListener.getSubscriptionCount(id)))
                .collect(Collectors.toList());
    }
}