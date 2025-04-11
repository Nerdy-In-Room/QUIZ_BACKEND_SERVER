package com.example.quiz.room.service;

import com.example.quiz.game.repository.GameRepository;
import com.example.quiz.global.type.Role;
import com.example.quiz.helper.RedissonTestConfig;
import com.example.quiz.room.dto.request.RoomCreateRequest;
import com.example.quiz.room.dto.response.RoomListResponse;
import com.example.quiz.room.dto.response.RoomResponse;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.mapper.RoomMapper;
import com.example.quiz.room.repository.RoomRepository;
import com.example.quiz.user.dto.request.LoginUserRequest;
import com.example.quiz.user.entity.User;
import com.example.quiz.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import(RedissonTestConfig.class)
class RoomProducerServiceIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private RedisTemplate<String, RoomResponse> roomCreateCacheTemplate;
    @Autowired
    private RedisTemplate<String, Integer> roomPeopleCacheTemplate;
    @Autowired
    private RoomProducerService roomProducerService;
    @Autowired
    private RedissonClient redissonClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        roomRepository.deleteAllInBatch();
        gameRepository.deleteAll();
        Set<String> keys = roomCreateCacheTemplate.keys("*");
        if (!keys.isEmpty()) {
            roomCreateCacheTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("입력이 올바를 때 방이 생성되는지 테스트한다.")
    void createRoomTest() {
        // given
        String uuid = "test-uuid";
        RoomCreateRequest roomCreateRequest = new RoomCreateRequest("Test Room", 1L, 8, 5, uuid);
        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@example.com", Role.USER);
        userRepository.save(new User(1L, "test@example.com_3002860612", "test@example.com", Role.USER));

        // when
        RoomResponse createdRoom = roomProducerService.createRoom(roomCreateRequest, loginUserRequest);

        // then
        assertThat(createdRoom).isNotNull();
        assertThat(createdRoom)
                .extracting("roomId", "roomName", "topicId", "maxPeople", "quizCount", "currentPeople")
                .contains(1L, "Test Room", 1L, 8, 5, 1);
    }

    @Test
    @DisplayName("락을 얻지 못했을 때 null 반환하는지 테스트한다.")
    void testCreateRoomLockNotAcquired() {
        // given
        String uuid = "test-uuid";
        RoomCreateRequest roomCreateRequest = new RoomCreateRequest("Test Room", 1L, 8, 5, uuid);
        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@example.com", Role.USER);
        userRepository.save(new User(1L, "test@example.com_3002860612", "test@example.com", Role.USER));

        new Thread(() -> {
            RLock rLock = redissonClient.getLock("room:create:test-uuid");
            rLock.lock(15, TimeUnit.SECONDS);
            log.info("lock : {}", rLock.isLocked());
        }).start();

        // when
        RoomResponse actualResponse = roomProducerService.createRoom(roomCreateRequest, loginUserRequest);

        // then
        assertNull(actualResponse);
    }

    @Test
    @DisplayName("방 중복 생성 요청이 됐을 때 생성된 방을 반환하는지 테스트한다.")
    void testCreateRoomCacheHit() {
        // given
        String uuid = "test-uuid";
        RoomCreateRequest roomCreateRequest = new RoomCreateRequest("Test Room", 1L, 8, 5, uuid);
        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@example.com", Role.USER);

        userRepository.save(new User(1L, "test@example.com_3002860612", "test@example.com", Role.USER));

        // when
        roomProducerService.createRoom(roomCreateRequest, loginUserRequest);
        RoomResponse actualResponse = roomProducerService.createRoom(roomCreateRequest, loginUserRequest);
        List<Room> rooms = roomRepository.findAll();

        // then
        assertNotNull(actualResponse);
        assertThat(actualResponse)
                .extracting("roomId", "roomName", "topicId", "maxPeople", "quizCount", "currentPeople")
                .contains(1L, "Test Room", 1L, 8, 5, 1);
        assertEquals(1, rooms.size());
    }

    @Test
    @DisplayName("다른 UUID로 동시 요청 시 분산락이 서로 간섭하지 않고 작동하는지 테스트한다.")
    void testDistributeLockWithDifferentUUIDs() throws InterruptedException {
        // given
        String uuid1 = "uuid-A";
        String uuid2 = "uuid-B";

        RoomCreateRequest request1 = new RoomCreateRequest("Room A", 1L, 8, 5, uuid1);
        RoomCreateRequest request2 = new RoomCreateRequest("Room B", 2L, 8, 5, uuid2);

        LoginUserRequest user1 = new LoginUserRequest(1L, "user1@example.com", Role.USER);
        LoginUserRequest user2 = new LoginUserRequest(2L, "user2@example.com", Role.USER);

        userRepository.save(new User(1L, "user1@example.com_hash", "user1@example.com", Role.USER));
        userRepository.save(new User(2L, "user2@example.com_hash", "user2@example.com", Role.USER));

        CountDownLatch latch = new CountDownLatch(2);
        List<Long> startTimes = Collections.synchronizedList(new ArrayList<>());
        List<RoomResponse> results = Collections.synchronizedList(new ArrayList<>());

        // when
        Runnable task1 = () -> {
            startTimes.add(System.currentTimeMillis());
            RoomResponse res = roomProducerService.createRoom(request1, user1);
            results.add(res);
            latch.countDown();
        };
        Runnable task2 = () -> {
            startTimes.add(System.currentTimeMillis());
            RoomResponse res = roomProducerService.createRoom(request2, user2);
            results.add(res);
            latch.countDown();
        };

        new Thread(task1).start();
        new Thread(task2).start();

        latch.await();

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isNotNull();
        assertThat(results.get(1)).isNotNull();
        assertThat(results.get(0).roomId()).isNotEqualTo(results.get(1).roomId());

        long diff = Math.abs(startTimes.get(0) - startTimes.get(1));
        assertThat(diff).isLessThan(100);
    }

    @Test
    @DisplayName("방 목록을 잘 반환하는지 테스트한다.")
    void getRoomListTest() {
        // given
        for (long i = 1; i <= 20; i++) {
            String ROOM_ID_PREFIX = "roomId:";
            RoomCreateRequest request = new RoomCreateRequest("room" + i, 1L, 8, 8, "UUID" + i);
            User user = userRepository.save(new User("test" + i, "test" + i + "@sample.com", Role.USER));
            LoginUserRequest loginUserRequest = new LoginUserRequest(user.getId(), user.getEmail(), Role.USER);
            RoomResponse roomResponse = roomProducerService.createRoom(request, loginUserRequest);
            roomPeopleCacheTemplate.opsForValue().set(ROOM_ID_PREFIX + roomResponse.roomId(), 1);
        }

        int index = 1;

        // when
        Page<RoomListResponse> list = roomProducerService.roomList(index);

        // then
        assertEquals(20, list.getTotalElements());
        assertEquals(2, list.getTotalPages());
        assertEquals(10, list.getContent().size());
    }
}
