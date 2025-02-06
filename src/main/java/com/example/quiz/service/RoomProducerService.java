package com.example.quiz.service;

import com.example.quiz.dto.User.LoginUserRequest;
import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.response.RoomListResponse;
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
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomProducerService {
    private static final int PAGE_SIZE = 10;

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;

    private final RedissonClient redissonClient;
    private final RedisTemplate<Long, Integer> roomOccupancyCacheTemplate;
    private final RedisTemplate<String, RoomResponse> roomCreateCacheTemplate;

    public RoomResponse createRoom(RoomCreateRequest roomRequest, LoginUserRequest loginUserRequest) throws IllegalAccessException {
        RoomResponse roomResponse = null;
        String lockKey = "room:create:" + roomRequest.UUID();

        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                roomResponse = roomCreateCacheTemplate.opsForValue().get(roomRequest.UUID());

                if (roomResponse != null) {

                    return roomResponse;
                }

                Room savedRoom = saveRoom(roomRequest, loginUserRequest);
                createGameWithMasterUser(savedRoom.getRoomId(), loginUserRequest);

                roomResponse = RoomMapper.INSTANCE.RoomToRoomResponse(savedRoom);
                roomCreateCacheTemplate.opsForValue().set(roomRequest.UUID(), roomResponse, 1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            log.error("Lock acquisition interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }

        return roomResponse;
    }

    public Page<RoomListResponse> roomList(int index) {
        Pageable pageable = PageRequest.of(index, PAGE_SIZE, Sort.by("roomId").descending());

        List<RoomListResponse> responses = getRoomList(pageable);

        return new PageImpl<>(responses, pageable, responses.size());
    }

    private InGameUser findUser(long roomId, LoginUserRequest loginUserRequest) throws IllegalAccessException {
        User user = userRepository.findById(loginUserRequest.userId()).orElseThrow(IllegalAccessException::new);

        return new InGameUser(user.getId(), roomId, user.getEmail(), Role.ADMIN, false);
    }

    private List<RoomListResponse> getRoomList(Pageable pageable) {
        return roomRepository.findAllByRemoveStatus(false, pageable)
                .stream()
                .map(room -> {
                    Integer currentPeople = roomOccupancyCacheTemplate.opsForValue().get(room.getRoomId());

                    return currentPeople != null
                            ? RoomMapper.INSTANCE.RoomToRoomListResponse(room, currentPeople)
                            : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private Room saveRoom(RoomCreateRequest roomRequest, LoginUserRequest loginUserRequest) {
        Room room = RoomMapper.INSTANCE.RoomCreateRequestToRoom(roomRequest, loginUserRequest.email());

        return roomRepository.save(room);
    }

    private void createGameWithMasterUser(Long roomId, LoginUserRequest loginUserRequest) throws IllegalAccessException {
        InGameUser masterUser = findUser(roomId, loginUserRequest);
        Game game = new Game(String.valueOf(roomId), roomId, 1, false, new HashSet<>());
        game.getGameUser().add(masterUser);
        gameRepository.save(game);
    }
}