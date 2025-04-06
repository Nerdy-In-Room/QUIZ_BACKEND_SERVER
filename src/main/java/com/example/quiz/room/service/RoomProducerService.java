package com.example.quiz.room.service;

import com.example.quiz.global.exception.general.GeneralErrorCode;
import com.example.quiz.global.exception.general.GeneralErrorException;
import com.example.quiz.global.type.Role;
import com.example.quiz.game.entity.Game;
import com.example.quiz.game.model.InGameUser;
import com.example.quiz.game.repository.GameRepository;
import com.example.quiz.room.dto.request.RoomCreateRequest;
import com.example.quiz.room.dto.response.RoomListResponse;
import com.example.quiz.room.dto.response.RoomResponse;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.mapper.RoomMapper;
import com.example.quiz.room.repository.RoomRepository;
import com.example.quiz.room.validation.RoomCreateValidation;
import com.example.quiz.user.dto.request.LoginUserRequest;
import com.example.quiz.user.entity.User;
import com.example.quiz.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomProducerService {
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;

    private static final int PAGE_SIZE = 10;
    private final String ROOM_ID_PREFIX = "roomId:";
    private final String ROOM_CREATE_LOCK_PREFIX = "room:create:";

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Integer> roomPeopleCacheTemplate;
    private final RedisTemplate<String, RoomResponse> roomCreateCacheTemplate;

    public RoomResponse createRoom(RoomCreateRequest roomRequest, LoginUserRequest loginUserRequest) {
        User user = findUser(loginUserRequest);
        RoomResponse roomResponse = null;
        String lockKey = ROOM_CREATE_LOCK_PREFIX + roomRequest.UUID();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                roomResponse = roomCreateCacheTemplate.opsForValue().get(roomRequest.UUID());

                if (roomResponse != null) {

                    return roomResponse;
                }

                Room savedRoom = saveRoom(roomRequest, loginUserRequest);
                createGameWithMasterUser(savedRoom.getRoomId(), user);

                roomResponse = RoomMapper.INSTANCE.RoomToRoomResponse(savedRoom);
                roomCreateCacheTemplate.opsForValue().set(roomRequest.UUID(), roomResponse, 1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            log.error("Lock acquisition interrupted: {}", e.getMessage());
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return roomResponse;
    }

    public Page<RoomListResponse> roomList(int index) {
        Pageable pageable = PageRequest.of(index, PAGE_SIZE, Sort.by("roomId").descending());

        List<RoomListResponse> responses = getRoomList(pageable);

        return new PageImpl<>(responses, pageable, responses.size());
    }

    private User findUser(LoginUserRequest loginUserRequest) {
        if (loginUserRequest == null) {

            throw new GeneralErrorException(GeneralErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findById(loginUserRequest.userId()).orElseThrow(() -> new GeneralErrorException(GeneralErrorCode.USER_NOT_FOUND));
    }

    private InGameUser createInGameUser(long roomId, User loginUser) {

        return new InGameUser(loginUser.getId(), roomId, loginUser.getEmail(), Role.ADMIN, false);
    }

    private List<RoomListResponse> getRoomList(Pageable pageable) {
        return roomRepository.findAllByRemoveStatus(false, pageable)
                .stream()
                .map(room -> {
                    Integer currentPeople = roomPeopleCacheTemplate.opsForValue().get(ROOM_ID_PREFIX + room.getRoomId());

                    return currentPeople != null
                            ? RoomMapper.INSTANCE.RoomToRoomListResponse(room, currentPeople)
                            : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private Room saveRoom(RoomCreateRequest roomRequest, LoginUserRequest loginUserRequest) {
        RoomCreateValidation.validateTopicId(roomRequest.topicId());
        RoomCreateValidation.validateRoomName(roomRequest.roomName());
        RoomCreateValidation.validateMaxPeople(roomRequest.maxPeople());
        RoomCreateValidation.validateQuizCount(roomRequest.quizCount());

        Room room = RoomMapper.INSTANCE.RoomCreateRequestToRoom(roomRequest, loginUserRequest.email());

        return roomRepository.save(room);
    }

    private void createGameWithMasterUser(Long roomId, User loginUser) {
        InGameUser masterUser = createInGameUser(roomId, loginUser);
        Game game = new Game(String.valueOf(roomId), roomId, 1, false, new HashSet<>());
        game.getGameUser().add(masterUser);
        gameRepository.save(game);
    }
}
