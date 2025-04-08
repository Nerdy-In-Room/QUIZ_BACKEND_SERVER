package com.example.quiz.global.config.stompConfig;

import com.example.quiz.game.entity.Game;
import com.example.quiz.game.model.InGameUser;
import com.example.quiz.game.repository.GameRepository;
import com.example.quiz.global.config.cacheConfig.redis.RedisEventPublisher;
import com.example.quiz.global.type.Role;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.dto.response.ChangeCurrentPeopleResponse;
import com.example.quiz.room.repository.RoomRepository;
import com.example.quiz.user.dto.request.LoginUserRequest;
import com.example.quiz.user.entity.User;
import com.example.quiz.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class StompEventListenerIntegrationTest {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Long> alreadyInGameUserCacheTemplate;

    @Autowired
    private RedisTemplate<String, Integer> roomPeopleCacheTemplate;

    @Autowired
    private Map<Long, AtomicInteger> roomSubscriptionCount;

    @SpyBean
    private RedisEventPublisher redisEventPublisher;

    @Test
    @DisplayName("방 인원이 0명이 되면 방이 삭제된다.")
    void noInGameUSerCleanRoom() {
        // given
        long userId = 1L;

        String email = "tset@sample.com";
        String username = email + "_1234";
        User user = userRepository.save(new User(username, email, Role.USER));
        Room room = roomRepository.save(new Room(1L,1L, "test room", 8, 8, false, "test@test.com"));

        Set<InGameUser> gameUserSet = new HashSet<>();
        gameUserSet.add(new InGameUser(userId, room.getRoomId(), user.getEmail(), Role.USER, false));
        gameRepository.save(new Game(String.valueOf(room.getRoomId()), room.getRoomId(), 1, false, gameUserSet));

        alreadyInGameUserCacheTemplate.opsForValue().set("userId:" + userId, room.getRoomId());
        roomPeopleCacheTemplate.opsForValue().set("roomId:" + room.getRoomId(), 1);
        roomSubscriptionCount.put(room.getRoomId(), new AtomicInteger(1));

        LoginUserRequest loginUser = new LoginUserRequest(userId, user.getEmail(), Role.USER);

        SimpMessageHeaderAccessor accessor = StompHeaderAccessor.create();
        accessor.setSessionAttributes(Map.of("loginUser", loginUser));
        accessor.setSessionId("test-session-id");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message);

        // when
        eventPublisher.publishEvent(event);

        // then
        assertThat(alreadyInGameUserCacheTemplate.opsForValue().get("userId:" + userId)).isNull();
        assertThat(roomPeopleCacheTemplate.opsForValue().get("roomId:" + room.getRoomId())).isNull();
        assertThat(roomSubscriptionCount.containsKey(room.getRoomId())).isFalse();
        assertThat(gameRepository.findById(String.valueOf(room.getRoomId()))).isEmpty();
        assertThat(roomRepository.findById(room.getRoomId()).get().getRemoveStatus()).isTrue();

        verify(redisEventPublisher, times(1)).publishChangeCurrentPeople(
                eq("change-roomList-channel"),
                argThat(change -> change.getRoomId() == room.getRoomId() && change.getCurrentPeople() == 0)
        );
    }

    @Test
    @DisplayName("방에 여러 명이 있을 때 한 명이 나가면 방은 삭제되지 않고 인원만 감소한다.")
    void testHandleSessionUnsubscribeEvent_MultipleUsers() {
        // given
        long userIdLeaving = 1L;
        String email = "user1@test.com";

        User user = userRepository.save(new User(email + "_1234", email, Role.USER));
        Room room = roomRepository.save(new Room(1L, 1L, "multi-user room", 8, 8, false, "master@test.com"));
        long roomId = room.getRoomId();

        Set<InGameUser> gameUsers = new HashSet<>();
        gameUsers.add(new InGameUser(userIdLeaving, roomId, email, Role.USER, false));
        for (long i = 2; i <= 7; i++) {
            gameUsers.add(new InGameUser(i, roomId, "user" + i + "@test.com", Role.USER, false));
        }

        gameRepository.save(new Game(String.valueOf(roomId), roomId, 7, false, gameUsers));

        alreadyInGameUserCacheTemplate.opsForValue().set("userId:" + userIdLeaving, roomId);
        roomPeopleCacheTemplate.opsForValue().set("roomId:" + roomId, 7);
        roomSubscriptionCount.put(roomId, new AtomicInteger(7));

        LoginUserRequest loginUser = new LoginUserRequest(userIdLeaving, email, Role.USER);
        SimpMessageHeaderAccessor accessor = StompHeaderAccessor.create();
        accessor.setSessionAttributes(Map.of("loginUser", loginUser));
        accessor.setSessionId("test-session-id");

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message);

        // when
        eventPublisher.publishEvent(event);

        // then
        assertThat(alreadyInGameUserCacheTemplate.opsForValue().get("userId:" + userIdLeaving)).isNull();
        assertThat(roomPeopleCacheTemplate.opsForValue().get("roomId:" + roomId)).isEqualTo(6);
        assertThat(roomSubscriptionCount.get(roomId).get()).isEqualTo(6);
        assertThat(roomRepository.findById(roomId)).isPresent();
        assertThat(roomRepository.findById(roomId).get().getRemoveStatus()).isFalse();
        assertThat(gameRepository.findById(String.valueOf(roomId))).isPresent();

        verify(redisEventPublisher, times(1)).publishChangeCurrentPeople(
                eq("change-roomList-channel"),
                argThat(change -> change.getRoomId() == roomId && change.getCurrentPeople() == 6)
        );
    }

    @Test
    @DisplayName("여러 유저가 순차적으로 퇴장하면 마지막에 방이 삭제된다.")
    void testHandleSessionUnsubscribeEvent_MultipleUsersSequentialLeave() {
        // given
        User user1 = userRepository.save(new User("user1_123", "user1@test.com", Role.ADMIN));
        User user2 = userRepository.save(new User("user2_123", "user2@test.com", Role.USER));
        User user3 = userRepository.save(new User("user3_123", "user3@test.com", Role.USER));

        Room room = roomRepository.save(new Room(1L, 1L, "test room", 8, 8, false, "master@test.com"));
        long roomId = room.getRoomId();

        Set<InGameUser> gameUsers = new HashSet<>();
        gameUsers.add(new InGameUser(user1.getId(), roomId, user1.getEmail(), Role.USER, false));
        gameUsers.add(new InGameUser(user2.getId(), roomId, user2.getEmail(), Role.USER, false));
        gameUsers.add(new InGameUser(user3.getId(), roomId, user3.getEmail(), Role.USER, false));

        gameRepository.save(new Game(String.valueOf(roomId), roomId, 3, false, gameUsers));
        roomPeopleCacheTemplate.opsForValue().set("roomId:" + roomId, 3);
        roomSubscriptionCount.put(roomId, new AtomicInteger(3));
        alreadyInGameUserCacheTemplate.opsForValue().set("userId:" + user1.getId(), roomId);
        alreadyInGameUserCacheTemplate.opsForValue().set("userId:" + user2.getId(), roomId);
        alreadyInGameUserCacheTemplate.opsForValue().set("userId:" + user3.getId(), roomId);

        // when
        List<User> users = List.of(user1, user2, user3);
        for (User user : users) {
            LoginUserRequest loginUser = new LoginUserRequest(user.getId(), user.getEmail(), Role.USER);
            SimpMessageHeaderAccessor accessor = StompHeaderAccessor.create();
            accessor.setSessionAttributes(Map.of("loginUser", loginUser));
            accessor.setSessionId("session-" + user.getId());

            Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
            SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message);

            eventPublisher.publishEvent(event);
        }

        // then
        assertThat(roomSubscriptionCount.containsKey(roomId)).isFalse();
        assertThat(roomPeopleCacheTemplate.opsForValue().get("roomId:" + roomId)).isNull();
        assertThat(gameRepository.findById(String.valueOf(roomId))).isEmpty();
        assertThat(roomRepository.findById(roomId)).isPresent();

        Room deletedRoom = roomRepository.findById(roomId).orElse(null);
        assertThat(deletedRoom).isNotNull();
        assertThat(deletedRoom.getRemoveStatus()).isTrue();

        verify(redisEventPublisher, times(3)).publishChangeCurrentPeople(
                eq("change-roomList-channel"),
                any(ChangeCurrentPeopleResponse.class)
        );
    }
}