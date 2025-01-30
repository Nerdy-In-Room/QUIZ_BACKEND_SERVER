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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final ApplicationEventPublisher eventPublisher;

    private final Cache<String, RoomResponse> roomCreateCache;
    private final Map<Long, AtomicInteger> roomSubscriptionCount;

    private final Lock lock = new ReentrantLock();

    public RoomResponse createRoom(RoomCreateRequest roomRequest, LoginUserRequest loginUserRequest) throws IllegalAccessException {
        RoomResponse roomResponse;

        lock.lock();
        try {
            roomResponse = roomCreateCache.getIfPresent(roomRequest.UUID());

            if (roomResponse != null) {
                log.info("already room");

                return roomResponse;
            }

            Room savedRoom = saveRoom(roomRequest, loginUserRequest);
            createGameWithMasterUser(savedRoom.getRoomId(), loginUserRequest);
            initializeRoomState(savedRoom.getRoomId());

            roomResponse = RoomMapper.INSTANCE.RoomToRoomResponse(savedRoom);
            roomCreateCache.put(roomRequest.UUID(), roomResponse);
        } finally {
            lock.unlock();
        }

        publishRoomCreatedEvent(roomResponse);

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
                    AtomicInteger count = roomSubscriptionCount.get(room.getRoomId());
                    Integer currentPeople = (count != null) ? count.get() : null;

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
        Game game = new Game(String.valueOf(roomId), 1, false, new HashSet<>());
        game.getGameUser().add(masterUser);
        gameRepository.save(game);
    }

    private void initializeRoomState(Long roomId) {
        roomSubscriptionCount.put(roomId, new AtomicInteger(0));
    }

    private void publishRoomCreatedEvent(RoomResponse roomResponse) {
        eventPublisher.publishEvent(roomResponse);
    }
}