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
import com.example.quiz.entity.user.User;
import com.example.quiz.enums.Role;
import com.example.quiz.mapper.RoomMapper;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.RoomRepository;
import com.example.quiz.repository.UserRepository;
import com.example.quiz.vo.InGameUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final RoomLockManager roomLockManager;

    private final String LOCK_PREFIX = "LOCK:";
    private final String ROOM_ID_PREFIX = "roomId:";
    private final String USER_ID_PREFIX = "userId:";
    private final String REDIS_CREATE_ROOM_CHANNEL = "create-room-channel";
    private final String REDIS_CHANGE_ROOM_LIST_CHANNEL = "change-roomList-channel";

    private final RedissonClient redissonClient;
    private final RedisEventPublisher redisEventPublisher;
    private final Map<Long, AtomicInteger> roomSubscriptionCount;
    private final RedisTemplate<String, Integer> roomOccupancyCacheTemplate;
    private final RedisTemplate<String, Long> alreadyInGameUserCacheTemplate;

    public RoomEnterResponse enterRoom(long roomId, LoginUserRequest loginUserRequest, String status) throws IllegalArgumentException, IllegalAccessException {
        validateLoginUser(loginUserRequest);
        checkAlreadyInGameUserDifferentRoom(loginUserRequest.userId(), roomId);

        ReentrantLock lock = roomLockManager.getLock(roomId);
        lock.lock();

        try {
            Room room = findRoomById(roomId);
            Game game = findGameByRoomId(roomId);
            InGameUser inGameUser = findUser(roomId, loginUserRequest);
            // 방 삭제 여부 확인
            if(validateRoom(roomId)) {
                return RoomMapper.INSTANCE.RoomToRoomEnterResponse(room, inGameUser, game.getGameUser());
            }

            if (isUserAlreadyInGameSameRoom(roomId, loginUserRequest.userId())) {
                return RoomMapper.INSTANCE.RoomToRoomEnterResponse(room, inGameUser, game.getGameUser());
            }

            int currentCount = incrementSubscriptionCount(roomId, loginUserRequest.userId());

            if (room.getMasterEmail().equals(loginUserRequest.email())) {
                publishRoomCreatedEvent(RoomMapper.INSTANCE.RoomToRoomResponse(room));
                addUserToGame(game, inGameUser, roomId, currentCount);
                simpMessagingTemplate.convertAndSend("/pub/room/" + roomId, inGameUser);
                return RoomMapper.INSTANCE.RoomToRoomEnterResponse(room, inGameUser, game.getGameUser());
            }

            addUserToGame(game, inGameUser, roomId, currentCount);
            simpMessagingTemplate.convertAndSend("/pub/room/" + roomId, inGameUser);

            return RoomMapper.INSTANCE.RoomToRoomEnterResponse(room, inGameUser, game.getGameUser());
        } finally {
            lock.unlock();
        }
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

    private InGameUser findUser(long roomId, LoginUserRequest loginUserRequest) throws IllegalArgumentException {
        User user = userRepository.findById(loginUserRequest.userId()).orElseThrow(IllegalArgumentException::new);
        Room room = findRoomById(roomId);

        if (loginUserRequest.email().equals(room.getMasterEmail())) {
            return new InGameUser(loginUserRequest.userId(), roomId, user.getEmail(), Role.ADMIN, false);
        }

        return new InGameUser(loginUserRequest.userId(), roomId, user.getEmail(), Role.USER, false);
    }

    private int incrementSubscriptionCount(Long roomId, Long userId) {
        if (!roomSubscriptionCount.containsKey(roomId)) {
            roomSubscriptionCount.put(roomId, new AtomicInteger(1));
            roomOccupancyCacheTemplate.opsForValue().set(ROOM_ID_PREFIX + roomId, 1);
            alreadyInGameUserCacheTemplate.opsForValue().set(USER_ID_PREFIX + userId, roomId);

            return 1;
        }

        return roomSubscriptionCount.get(roomId).updateAndGet(c -> {
            if (c >= 8) {
                throw new RuntimeException("Room capacity reached : " + roomId);
            }

            if (c == 0) {
                return c;
            }

            roomOccupancyCacheTemplate.opsForValue().increment(ROOM_ID_PREFIX + roomId);
            alreadyInGameUserCacheTemplate.opsForValue().set(USER_ID_PREFIX + userId, roomId);

            return c + 1;
        });
    }

    private boolean validateRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        return room.getRemoveStatus();
    }

    private void validateLoginUser(LoginUserRequest loginUserRequest) throws IllegalArgumentException {
        if (loginUserRequest == null) {
            throw new IllegalArgumentException("Login user is null");
        }
    }

    private void checkAlreadyInGameUserDifferentRoom(long userId, long roomId) {
        String key = LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(key);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                Long findRoomId = alreadyInGameUserCacheTemplate.opsForValue().get(USER_ID_PREFIX + key);

                if (findRoomId != null && findRoomId != roomId) {
                    throw new RuntimeException("already in game user another room: " + userId);
                }
            }
        } catch (InterruptedException e) {
            log.error("Lock acquisition interrupted: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private Room findRoomById(long roomId) {
        return roomRepository.findById(roomId)
                .orElse(new Room(null, null, null, null, null, null, null));
    }

    private Game findGameByRoomId(long roomId) {
        return gameRepository.findById(String.valueOf(roomId))
                .orElse(new Game(null, null, null, null, new HashSet<>()));
    }

    private boolean isUserAlreadyInGameSameRoom(long roomId, long userId) {
        String key = LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(key);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                Long findRoomId = alreadyInGameUserCacheTemplate.opsForValue().get(USER_ID_PREFIX + key);

                return findRoomId != null && findRoomId == roomId;
            }
        } catch (InterruptedException e) {
            log.error("Lock acquisition interrupted: {}", e.getMessage());
        } finally {
            lock.unlock();
        }

        return false;
    }

    private void addUserToGame(Game game, InGameUser inGameUser, long roomId, int currentCount) {
        game.getGameUser().add(inGameUser);
        game.changeCurrentParticipantsNo(game.getGameUser().size());
        gameRepository.save(game);

        publishChangeCurrentOccupancies(roomId, currentCount);
    }

    private void publishRoomCreatedEvent(RoomResponse roomResponse) {
        redisEventPublisher.publishCreatEvent(REDIS_CREATE_ROOM_CHANNEL, roomResponse);
    }

    private void publishChangeCurrentOccupancies(long roomId, int currentCount) {
        redisEventPublisher.publishChangeCurrentOccupancies(REDIS_CHANGE_ROOM_LIST_CHANNEL, new ChangeCurrentOccupancies(roomId, currentCount));
    }
}