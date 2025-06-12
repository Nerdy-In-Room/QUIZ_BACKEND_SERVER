package com.example.quiz.room.service;

import com.example.quiz.game.model.InGameUser;
import com.example.quiz.global.type.Role;
import com.example.quiz.room.dto.request.RoomCreateRequest;
import com.example.quiz.room.dto.request.RoomModifyRequest;
import com.example.quiz.room.dto.response.RoomEnterResponse;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.exception.RoomErrorException;
import com.example.quiz.room.repository.RoomRepository;
import com.example.quiz.user.dto.request.LoginUserRequest;
import com.example.quiz.user.entity.User;
import com.example.quiz.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class RoomServiceIntegrationTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private Map<Long, Long> alreadyInGameUser;
    @Autowired
    private Map<Long, AtomicInteger> roomSubscriptionCount;
    @Autowired
    private RoomProducerService roomProducerService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private RedisTemplate<String, Long> alreadyInGameUserCacheTemplate;
    @Autowired
    private RedisTemplate<String, Integer> roomPeopleCacheTemplate;
    @SpyBean
    private SimpMessagingTemplate simpMessagingTemplate;

    @AfterEach
    void tearDown() {
        roomRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        alreadyInGameUser.clear();
        roomSubscriptionCount.clear();
        Set<String> keys1 = alreadyInGameUserCacheTemplate.keys("*");
        if (!keys1.isEmpty()) {
            alreadyInGameUserCacheTemplate.delete(keys1);
        }
        Set<String> keys2 = roomPeopleCacheTemplate.keys("*");
        if (!keys2.isEmpty()) {
            roomPeopleCacheTemplate.delete(keys2);
        }
    }

    @Test
    @DisplayName("방을 만든 후 생성된 방 정보가 브로드캐스팅된다.")
    void broadcastingInitRoom() {
        // given
        User master = userRepository.save(new User("master1", "email1", Role.ADMIN));
        long roomId = roomProducerService.createRoom(new RoomCreateRequest("room1", 1L, 8, 8, "1"), new LoginUserRequest(master.getId(), "email1", Role.ADMIN)).roomId();
        LoginUserRequest loginUserRequest = new LoginUserRequest(master.getId(), master.getEmail(), Role.ADMIN);

        // when
        RoomEnterResponse roomEnterResponse = roomService.enterRoom(roomId, loginUserRequest);

        // then
        ArgumentCaptor<InGameUser> captor = ArgumentCaptor.forClass(InGameUser.class);

        verify(simpMessagingTemplate, times(1))
                .convertAndSend(eq("/pub/room/" + roomId), captor.capture());

        InGameUser captured = captor.getValue();
        assertEquals(master.getId(), captured.getId());
        assertEquals(roomId, captured.getRoomId());
        assertEquals(1, roomEnterResponse.participants().size());
        assertEquals(roomId, roomEnterResponse.inGameUser().getRoomId());
    }

    @Test
    @DisplayName("최대인원보다 현재인원이 적을 경우 방에 입장할 수 있다.")
    void enterRoom() {
        // given
        User master = userRepository.save(new User("master1", "email1", Role.ADMIN));
        User user = userRepository.save(new User("user", "email2", Role.USER));
        long roomId = roomProducerService.createRoom(new RoomCreateRequest("room1", 1L, 8, 8, "1"), new LoginUserRequest(master.getId(), "email1", Role.ADMIN)).roomId();
        roomSubscriptionCount.put(roomId, new AtomicInteger(1));
        LoginUserRequest loginUserRequest = new LoginUserRequest(user.getId(), user.getEmail(), Role.USER);

        // when
        RoomEnterResponse roomEnterResponse = roomService.enterRoom(roomId, loginUserRequest);

        // then
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(simpMessagingTemplate, times(1))
                .convertAndSend(destinationCaptor.capture(), payloadCaptor.capture());

        assertEquals("/pub/room/" + roomId, destinationCaptor.getValue());
        assertInstanceOf(InGameUser.class, payloadCaptor.getValue());
        InGameUser captured = (InGameUser) payloadCaptor.getValue();
        assertEquals(user.getId(), captured.getId());
        assertEquals(roomId, captured.getRoomId());
        assertEquals(2, roomEnterResponse.participants().size());
        assertEquals(roomId, roomEnterResponse.inGameUser().getRoomId());
    }

    @Test
    @DisplayName("정원이 가득 찬 방에는 입장할 수 없다.")
    void overPeopleInRoom() {
        User master = userRepository.save(new User("master1", "email1", Role.ADMIN));
        User user = userRepository.save(new User("user", "email2", Role.USER));
        long roomId = roomProducerService.createRoom(new RoomCreateRequest("room1", 1L, 8, 8, "1"), new LoginUserRequest(master.getId(), "email1", Role.ADMIN)).roomId();
        roomSubscriptionCount.put(roomId, new AtomicInteger(8));
        LoginUserRequest loginUserRequest = new LoginUserRequest(user.getId(), user.getEmail(), Role.USER);

        assertThatThrownBy(() -> roomService.enterRoom(roomId, loginUserRequest))
                .isInstanceOf(RoomErrorException.class)
                .hasMessage("정원이 초과되었습니다.");
    }

    @Test
    @DisplayName("방 최대 인원제한 테스트 - 동시성")
    void subscriptionTest() throws ExecutionException, InterruptedException {
        // given
        int TEST_THREAD = 100;
        List<User> userList = new ArrayList<>();
        for (long i = 1; i <= TEST_THREAD; i++) {
            userList.add(userRepository.save(new User("user" + i, "test@test.com" + i, Role.USER)));
        }
        User master1 = userRepository.save(new User("master1", "email1", Role.ADMIN));
        User master2 = userRepository.save(new User("master2", "email2", Role.ADMIN));

        long roomId1 = roomProducerService.createRoom(new RoomCreateRequest("room1", 1L, 8, 8, "1"), new LoginUserRequest(master1.getId(), "email1", Role.ADMIN)).roomId();
        long roomId2 = roomProducerService.createRoom(new RoomCreateRequest("room2", 1L, 8, 8, "2"), new LoginUserRequest(master2.getId(), "email2", Role.ADMIN)).roomId();

        roomSubscriptionCount.put(roomId1, new AtomicInteger(1));
        roomSubscriptionCount.put(roomId2, new AtomicInteger(1));

        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        CyclicBarrier cyclicBarrier = new CyclicBarrier(TEST_THREAD);
        ExecutorService es = Executors.newFixedThreadPool(TEST_THREAD);
        AtomicInteger failureCount = new AtomicInteger();

        // when
        for (User user : userList) {
            long id = user.getId();
            long targetRoomId = (id % 2 == 0) ? roomId2 : roomId1;

            tasks.add(CompletableFuture.runAsync(() -> {
                try {
                    cyclicBarrier.await();
                    roomService.enterRoom(targetRoomId, new LoginUserRequest(id, "email", Role.USER));
                } catch (Exception e) {
                    if (e.getMessage().equals("정원이 초과되었습니다.")) {
                        failureCount.incrementAndGet();
                    }
                }
            }, es));
        }

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();

        //then
        assertEquals(8, roomSubscriptionCount.get(roomId1).get());
        assertEquals(8, roomSubscriptionCount.get(roomId2).get());
        assertEquals(86, failureCount.get());

        es.shutdown();
    }

    @Test
    @DisplayName("방 수정을 할 수 있다.")
    void modifyRoomInfo() {
        User master = userRepository.save(new User("master1", "email1", Role.ADMIN));
        long roomId = roomProducerService.createRoom(new RoomCreateRequest("room1", 1L, 8, 8, "1"), new LoginUserRequest(master.getId(), "email1", Role.ADMIN)).roomId();
        RoomModifyRequest request = new RoomModifyRequest("modify room name", 2L, 6, 6);
        LoginUserRequest loginUserRequest = new LoginUserRequest(master.getId(), master.getEmail(), Role.ADMIN);

        // when
        roomService.modifyRoom(request, roomId, loginUserRequest);
        Room actualResponse = roomRepository.findById(roomId).get();

        // then
        assertThat(actualResponse)
                .extracting("roomName", "topicId", "maxPeople", "quizCount")
                .contains(request.roomName(), request.topicId(), request.maxPeople(), request.quizCount());
    }
}