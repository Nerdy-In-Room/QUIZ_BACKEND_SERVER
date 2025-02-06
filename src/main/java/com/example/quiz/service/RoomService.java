package com.example.quiz.service;

import com.example.quiz.config.cacheConfig.redis.RedisEventPublisher;
import com.example.quiz.dto.User.LoginUserRequest;
import com.example.quiz.dto.response.QuizRoomEnterResponse;
import com.example.quiz.dto.room.ChangeCurrentOccupancies;
import com.example.quiz.dto.room.request.RoomModifyRequest;
import com.example.quiz.dto.room.response.RoomEnterResponse;
import com.example.quiz.dto.room.response.RoomModifyResponse;
import com.example.quiz.dto.room.response.RoomResponse;
import com.example.quiz.entity.Game;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.User;
import com.example.quiz.enums.Role;
import com.example.quiz.mapper.RoomMapper;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.RoomRepository;
import com.example.quiz.repository.UserRepository;
import com.example.quiz.vo.InGameUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;
    private final ApplicationEventPublisher publisher;
    private final SimpMessagingTemplate simpMessagingTemplate;

    private final Map<Long, Long> alreadyInGameUser;
    private final RedisEventPublisher redisEventPublisher;
    private final Map<Long, AtomicInteger> roomSubscriptionCount;
    private final RedisTemplate<Long, Integer> roomOccupancyCacheTemplate;

    public RoomEnterResponse enterRoom(long roomId, LoginUserRequest loginUserRequest) throws IllegalAccessException {
        validateLoginUser(loginUserRequest);
        checkAlreadyInGameUser(roomId, loginUserRequest.userId());

        Room room = findRoomById(roomId);
        Game game = findGameByRoomId(roomId);
        InGameUser inGameUser = findUser(roomId, loginUserRequest);

        if (isUserAlreadyInGame(roomId, loginUserRequest.userId())) {
            return RoomMapper.INSTANCE.RoomToRoomEnterResponse(room, inGameUser, game.getGameUser());
        }

        if (room.getMasterEmail().equals(loginUserRequest.email())) {
            publishRoomCreatedEvent(RoomMapper.INSTANCE.RoomToRoomResponse(room));
        }

        int currentCount = incrementSubscriptionCount(roomId, loginUserRequest.userId());

        addUserToGame(game, inGameUser, roomId, currentCount);
        simpMessagingTemplate.convertAndSend("/pub/room/" + roomId, inGameUser);

        return RoomMapper.INSTANCE.RoomToRoomEnterResponse(room, inGameUser, game.getGameUser());
    }

    public QuizRoomEnterResponse enterQuizRoom(long roomId, LoginUserRequest loginUserRequest) throws IllegalAccessException {
        User user = userRepository.findById(loginUserRequest.userId()).orElseThrow(IllegalAccessException::new);
        Room room = findRoomById(roomId);

        InGameUser inGameUser = findUser(roomId, loginUserRequest);

        return RoomMapper.INSTANCE.RoomToQuizRoomEnterResponse(inGameUser, user, room);
    }

    @Transactional
    public RoomModifyResponse modifyRoom(RoomModifyRequest request, long roomId) throws IllegalAccessException {
        Room room = roomRepository.findById(roomId).orElseThrow(IllegalAccessException::new);
        room.changeRoomName(request.roomName());
        room.changeSubject(request.topicId());

        return new RoomModifyResponse(room.getRoomName(), room.getTopicId());
    }

    private InGameUser findUser(long roomId, LoginUserRequest loginUserRequest) throws IllegalAccessException {
        User user = userRepository.findById(loginUserRequest.userId()).orElseThrow(IllegalAccessException::new);
        Room room = findRoomById(roomId);

        if (loginUserRequest.email().equals(room.getMasterEmail())) {
            return new InGameUser(loginUserRequest.userId(), roomId, user.getEmail(), Role.ADMIN, false);
        }

        return new InGameUser(loginUserRequest.userId(), roomId, user.getEmail(), Role.USER, false);
    }

    private int incrementSubscriptionCount(Long roomId, Long userId) {
        if (!roomSubscriptionCount.containsKey(roomId)) {
            roomSubscriptionCount.put(roomId, new AtomicInteger(1));
            roomOccupancyCacheTemplate.opsForValue().set(roomId, 1);
            alreadyInGameUser.put(userId, roomId);

            return 1;
        }

        return roomSubscriptionCount.get(roomId).updateAndGet(c -> {
            if (c >= 8) {
                throw new RuntimeException("Room capacity reached : " + roomId);
            }

            roomOccupancyCacheTemplate.opsForValue().increment(roomId);
            alreadyInGameUser.put(userId, roomId);

            return c + 1;
        });
    }

    private void validateLoginUser(LoginUserRequest loginUserRequest) throws IllegalAccessException {
        if (loginUserRequest == null) {
            throw new IllegalAccessException("Login user is null");
        }
    }

    private void checkAlreadyInGameUser(long userId, long roomId) {
        alreadyInGameUser.compute(userId, (key, existingValue) -> {
            if (existingValue != null && existingValue != roomId) {
                throw new RuntimeException("already in game user: " + userId);
            }

            return existingValue;
        });
    }

    private Room findRoomById(long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found for id: " + roomId));
    }

    private Game findGameByRoomId(long roomId) {
        return gameRepository.findById(String.valueOf(roomId))
                .orElseThrow(() -> new IllegalArgumentException("Game not found for room id: " + roomId));
    }

    private boolean isUserAlreadyInGame(long roomId, long userId) {
        Long findRoomId = alreadyInGameUser.get(userId);

        return findRoomId != null && findRoomId == roomId;
    }

    private void addUserToGame(Game game, InGameUser inGameUser, long roomId, int currentCount) {
        game.getGameUser().add(inGameUser);
        game.changeCurrentParticipantsNo(game.getGameUser().size());
        gameRepository.save(game);

        redisEventPublisher.publishChangeCurrentOccupancies("change-occupancies-channel", new ChangeCurrentOccupancies(roomId, currentCount));
    }

    private void publishRoomCreatedEvent(RoomResponse roomResponse) {
        publisher.publishEvent(roomResponse);
    }
}