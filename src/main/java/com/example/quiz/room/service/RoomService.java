package com.example.quiz.room.service;

import com.example.quiz.game.entity.Game;
import com.example.quiz.game.exception.GameErrorCode;
import com.example.quiz.game.exception.GameErrorException;
import com.example.quiz.game.model.InGameUser;
import com.example.quiz.game.repository.GameRepository;
import com.example.quiz.global.config.RoomLockManager;
import com.example.quiz.global.config.cacheConfig.redis.RedisEventPublisher;
import com.example.quiz.global.exception.general.GeneralErrorCode;
import com.example.quiz.global.exception.general.GeneralErrorException;
import com.example.quiz.global.type.Role;
import com.example.quiz.room.dto.request.RoomModifyRequest;
import com.example.quiz.room.dto.response.QuizRoomEnterResponse;
import com.example.quiz.room.dto.response.RoomEnterResponse;
import com.example.quiz.room.dto.response.RoomModifyResponse;
import com.example.quiz.room.dto.response.RoomResponse;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.exception.RoomErrorCode;
import com.example.quiz.room.exception.RoomErrorException;
import com.example.quiz.room.mapper.RoomMapper;
import com.example.quiz.room.model.ChangeCurrentPeople;
import com.example.quiz.room.repository.RoomRepository;
import com.example.quiz.room.validation.RoomCreateValidation;
import com.example.quiz.user.dto.request.LoginUserRequest;
import com.example.quiz.user.entity.User;
import com.example.quiz.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final int INIT_ROOM_PEOPLE = 1;
    private final String LOCK_PREFIX = "LOCK:";
    private final String ROOM_ID_PREFIX = "roomId:";
    private final String USER_ID_PREFIX = "userId:";
    private final String REDIS_CREATE_ROOM_CHANNEL = "create-room-channel";
    private final String REDIS_CHANGE_ROOM_LIST_CHANNEL = "change-roomList-channel";

    private final RedissonClient redissonClient;
    private final RedisEventPublisher redisEventPublisher;
    private final Map<Long, AtomicInteger> roomSubscriptionCount;
    private final RedisTemplate<String, Integer> roomPeopleCacheTemplate;
    private final RedisTemplate<String, Long> alreadyInGameUserCacheTemplate;

    public RoomEnterResponse enterRoom(long roomId, LoginUserRequest loginUserRequest, String status) {
        validateLoginUser(loginUserRequest);
        checkAlreadyInGameUserDifferentRoom(loginUserRequest.userId(), roomId);

        ReentrantLock lock = roomLockManager.getLock(roomId);

        try {
            if (lock.tryLock(3, TimeUnit.SECONDS)) {
                Room room = findRoomById(roomId);
                Game game = findGameByRoomId(roomId);
                InGameUser inGameUser = findInGameUser(roomId, loginUserRequest);

                if (validateRoom(roomId)) {
                    return RoomMapper.INSTANCE.RoomToRoomEnterResponse(room, inGameUser, game.getGameUser());
                }

                if (isUserAlreadyInGameSameRoom(roomId, loginUserRequest.userId())) {
                    return RoomMapper.INSTANCE.RoomToRoomEnterResponse(room, inGameUser, game.getGameUser());
                }

                int currentCount = incrementSubscriptionCount(roomId, loginUserRequest.userId(), room.getMaxPeople());

                if (room.getMasterEmail().equals(loginUserRequest.email())) {
                    publishRoomCreatedEvent(RoomMapper.INSTANCE.RoomToRoomResponse(room));
                    simpMessagingTemplate.convertAndSend("/pub/room/" + roomId, inGameUser);

                    return RoomMapper.INSTANCE.RoomToRoomEnterResponse(room, inGameUser, game.getGameUser());
                }

                addUserToGame(game, inGameUser, roomId, currentCount);
                simpMessagingTemplate.convertAndSend("/pub/room/" + roomId, inGameUser);

                return RoomMapper.INSTANCE.RoomToRoomEnterResponse(room, inGameUser, game.getGameUser());
            }
        } catch (InterruptedException e) {
            log.error("방 입장 lock 중 인터럽트 발생: {}", e.getMessage());
        } finally {
            lock.unlock();
        }

        throw new RoomErrorException(RoomErrorCode.FAIL_ENTER_ROOM, roomId + " 방 입장에 실패했습니다.");
    }

    public QuizRoomEnterResponse enterQuizRoom(long roomId, LoginUserRequest loginUserRequest) {
        User user = findUser(loginUserRequest);
        Room room = findRoomById(roomId);
        InGameUser inGameUser = findInGameUser(roomId, loginUserRequest);

        return RoomMapper.INSTANCE.RoomToQuizRoomEnterResponse(inGameUser, user, room);
    }

    @Transactional
    public RoomModifyResponse modifyRoom(RoomModifyRequest request, long roomId, LoginUserRequest loginUserRequest) {
        Room room = findRoomById(roomId);
        findUser(loginUserRequest);

        validateIsUserMaster(loginUserRequest, room);
        validateNowPeople(roomId, request.maxPeople());
        modifyRoomValidation(request);

        room.changeRoomName(request.roomName());
        room.changeSubject(request.topicId());
        room.changeMaxPeople(request.maxPeople());
        room.changeQuizCount(request.quizCount());

        return new RoomModifyResponse(room.getRoomName(), room.getTopicId(), room.getMaxPeople(), room.getQuizCount());
    }

    private void validateIsUserMaster(LoginUserRequest loginUserRequest, Room room) {
        if (!loginUserRequest.role().equals(Role.ADMIN)) {
            throw new RoomErrorException(RoomErrorCode.FAIL_MODIFY_ROOM, "방장이 아닙니다.");
        }

        if (!loginUserRequest.email().equals(room.getMasterEmail())) {
            throw new RoomErrorException(RoomErrorCode.FAIL_MODIFY_ROOM, "현재 방과 다릅니다.");
        }
    }

    private InGameUser findInGameUser(long roomId, LoginUserRequest loginUserRequest) {
        User user = findUser(loginUserRequest);

        if (loginUserRequest.role().equals(Role.ADMIN)) {
            return new InGameUser(loginUserRequest.userId(), roomId, user.getEmail(), Role.ADMIN, false);
        }

        return new InGameUser(loginUserRequest.userId(), roomId, user.getEmail(), Role.USER, false);
    }

    private void validateNowPeople(Long roomId, Integer maxPeople) {
        Game game = findGameByRoomId(roomId);

        if (maxPeople == null || maxPeople < game.getGameUser().size()) {
            throw new RoomErrorException(RoomErrorCode.WRONG_MAX_PEOPLE);
        }
    }

    private User findUser(LoginUserRequest loginUserRequest) {
        return userRepository.findById(loginUserRequest.userId()).orElseThrow(() -> new GeneralErrorException(GeneralErrorCode.USER_NOT_FOUND));
    }

    private int incrementSubscriptionCount(Long roomId, Long userId, int maxUser) {
        if (!roomSubscriptionCount.containsKey(roomId)) {
            roomSubscriptionCount.put(roomId, new AtomicInteger(INIT_ROOM_PEOPLE));
            roomPeopleCacheTemplate.opsForValue().set(ROOM_ID_PREFIX + roomId, INIT_ROOM_PEOPLE);
            alreadyInGameUserCacheTemplate.opsForValue().set(USER_ID_PREFIX + userId, roomId);

            return 1;
        }

        return roomSubscriptionCount.get(roomId).updateAndGet(c -> {
            if (c >= maxUser) {
                throw new RoomErrorException(RoomErrorCode.MAX_ROOM);
            }

            if (c == 0) {
                return c;
            }

            roomPeopleCacheTemplate.opsForValue().increment(ROOM_ID_PREFIX + roomId);
            alreadyInGameUserCacheTemplate.opsForValue().set(USER_ID_PREFIX + userId, roomId);

            return c + 1;
        });
    }

    private boolean validateRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomErrorException(RoomErrorCode.NOT_FOUND_ROOM, ROOM_ID_PREFIX + roomId));

        return room.getRemoveStatus();
    }

    private void validateLoginUser(LoginUserRequest loginUserRequest) {
        if (loginUserRequest == null) {
            throw new GeneralErrorException(GeneralErrorCode.USER_NOT_FOUND);
        }
    }

    private void checkAlreadyInGameUserDifferentRoom(long userId, long roomId) {
        Long findRoomId = alreadyInGameUserCacheTemplate.opsForValue().get(USER_ID_PREFIX + userId);

        if (findRoomId != null && findRoomId != roomId) {
            throw new RoomErrorException(RoomErrorCode.ALREADY_IN_ANOTHER_ROOM);
        }

    }

    private Room findRoomById(long roomId) {
        return roomRepository.findById(roomId).orElseThrow(() -> new RoomErrorException(RoomErrorCode.NOT_FOUND_ROOM, "Room ID: " + roomId));
    }

    private Game findGameByRoomId(long roomId) {
        return gameRepository.findById(String.valueOf(roomId))
                .orElseThrow(() -> new GameErrorException(GameErrorCode.GAME_NOT_FOUND));
    }

    private boolean isUserAlreadyInGameSameRoom(long roomId, long userId) {
        Long findRoomId = alreadyInGameUserCacheTemplate.opsForValue().get(USER_ID_PREFIX + userId);

        return findRoomId != null && findRoomId == roomId;
    }

    private void modifyRoomValidation(RoomModifyRequest request) {
        RoomCreateValidation.validateTopicId(request.topicId());
        RoomCreateValidation.validateRoomName(request.roomName());
        RoomCreateValidation.validateMaxPeople(request.maxPeople());
        RoomCreateValidation.validateQuizCount(request.quizCount());
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
        redisEventPublisher.publishChangeCurrentPeople(REDIS_CHANGE_ROOM_LIST_CHANNEL, new ChangeCurrentPeople(roomId, currentCount));
    }
}