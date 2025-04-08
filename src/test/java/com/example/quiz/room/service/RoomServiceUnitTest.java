package com.example.quiz.room.service;

import com.example.quiz.game.entity.Game;
import com.example.quiz.game.model.InGameUser;
import com.example.quiz.game.repository.GameRepository;
import com.example.quiz.global.config.RoomLockManager;
import com.example.quiz.global.config.cacheConfig.redis.RedisEventPublisher;
import com.example.quiz.global.exception.general.GeneralErrorException;
import com.example.quiz.global.type.Role;
import com.example.quiz.room.dto.request.RoomModifyRequest;
import com.example.quiz.room.dto.response.RoomEnterResponse;
import com.example.quiz.room.dto.response.RoomModifyResponse;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.exception.RoomErrorException;
import com.example.quiz.room.repository.RoomRepository;
import com.example.quiz.user.dto.request.LoginUserRequest;
import com.example.quiz.user.entity.User;
import com.example.quiz.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceUnitTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;
    @Mock
    private RoomLockManager roomLockManager;

    @Mock
    private RedisEventPublisher redisEventPublisher;
    @Mock
    private RedisTemplate<String, Integer> roomPeopleCacheTemplate;
    @Mock
    private RedisTemplate<String, Long> alreadyInGameUserCacheTemplate;

    private RoomService roomService;
    private Map<Long, AtomicInteger> roomSubscriptionCount;

    @BeforeEach
    void setUp() {
        roomSubscriptionCount = new ConcurrentHashMap<>();
        roomService = new RoomService(
                userRepository, roomRepository, gameRepository, simpMessagingTemplate, roomLockManager,
                redisEventPublisher, roomSubscriptionCount,
                roomPeopleCacheTemplate, alreadyInGameUserCacheTemplate
        );
    }

    @Test
    @DisplayName("방장이 방을 만든 후 정상적으로 초기화된다.")
    void initRoom() {
        // given
        long roomId = 1L;
        long masterId = 1L;
        int INIT_ROOM_PEOPLE = 1;
        String masterEmail = "master@test.com";
        String userName = masterEmail + "_1234";

        Room room = new Room(roomId, 1L, "test room", 8, 8, false, "master@test.com");
        User masterUser = new User(masterId, userName, masterEmail, Role.ADMIN);
        LoginUserRequest loginUserRequest = new LoginUserRequest(masterId, masterEmail, Role.USER);

        Set<InGameUser> inGameUsers = new HashSet<>();
        InGameUser inGameUser = new InGameUser(1L, roomId, masterUser.getEmail(), Role.ADMIN, false);
        inGameUsers.add(inGameUser);
        Game game = new Game(String.valueOf(roomId), 1L, 1, false, inGameUsers);

        ReentrantLock mockLock = new ReentrantLock();

        given(userRepository.findById(roomId)).willReturn(Optional.of(masterUser));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(gameRepository.findById(String.valueOf(roomId))).willReturn(Optional.of(game));
        given(roomLockManager.getLock(roomId)).willReturn(mockLock);

        ValueOperations<String, Integer> intValueOps = mock(ValueOperations.class);
        ValueOperations<String, Long> longValueOps = mock(ValueOperations.class);

        given(roomPeopleCacheTemplate.opsForValue()).willReturn(intValueOps);
        given(alreadyInGameUserCacheTemplate.opsForValue()).willReturn(longValueOps);

        // when
        RoomEnterResponse response = roomService.enterRoom(roomId, loginUserRequest, "master");

        // then
        assertThat(response).isNotNull();
        assertThat(response.roomId()).isEqualTo(roomId);
        assertThat(roomSubscriptionCount.get(roomId).get()).isEqualTo(1);

        ArgumentCaptor<InGameUser> captor = ArgumentCaptor.forClass(InGameUser.class);

        verify(roomPeopleCacheTemplate.opsForValue()).set("roomId:" + roomId, INIT_ROOM_PEOPLE);
        verify(alreadyInGameUserCacheTemplate.opsForValue()).set("userId:" + masterId, roomId);
        verify(simpMessagingTemplate).convertAndSend(eq("/pub/room/" + roomId), captor.capture());

        InGameUser actual = captor.getValue();
        assertThat(actual.getId()).isEqualTo(masterId);
        assertThat(actual.getUsername()).isEqualTo(masterUser.getEmail());
    }

    @Test
    @DisplayName("방에 입장할 수 있다.")
    void enterRoom() {
        // given
        long roomId = 1L;
        long userId = 2L;
        String masterEmail = "master@test.com";
        String userEmail = "sample@test.com";

        Room room = new Room(roomId, 1L, "test room", 8, 8, false, "master@test.com");
        User masterUser = new User(1L, masterEmail + "_1234", masterEmail, Role.ADMIN);
        User normalUser = new User(userId, userEmail + "_1234", userEmail, Role.USER);
        LoginUserRequest loginUserRequest = new LoginUserRequest(userId, userEmail, Role.USER);

        Set<InGameUser> inGameUsers = new HashSet<>();
        inGameUsers.add(new InGameUser(1L, roomId, masterUser.getEmail(), Role.ADMIN, false));
        Game game = new Game(String.valueOf(roomId), 1L, 1, false, inGameUsers);

        ReentrantLock mockLock = new ReentrantLock();
        roomSubscriptionCount.put(roomId, new AtomicInteger(1));

        given(userRepository.findById(userId)).willReturn(Optional.of(normalUser));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(gameRepository.findById(String.valueOf(roomId))).willReturn(Optional.of(game));
        given(roomLockManager.getLock(roomId)).willReturn(mockLock);

        ValueOperations<String, Integer> intValueOps = mock(ValueOperations.class);
        ValueOperations<String, Long> longValueOps = mock(ValueOperations.class);

        given(roomPeopleCacheTemplate.opsForValue()).willReturn(intValueOps);
        given(alreadyInGameUserCacheTemplate.opsForValue()).willReturn(longValueOps);

        // when
        RoomEnterResponse response = roomService.enterRoom(roomId, loginUserRequest, "");

        // then
        assertThat(response).isNotNull();
        assertThat(response.roomId()).isEqualTo(roomId);
        assertThat(roomSubscriptionCount.get(roomId).get()).isEqualTo(2);

        ArgumentCaptor<InGameUser> captor = ArgumentCaptor.forClass(InGameUser.class);

        verify(roomPeopleCacheTemplate.opsForValue()).increment("roomId:" + roomId);
        verify(alreadyInGameUserCacheTemplate.opsForValue()).set("userId:" + userId, roomId);
        verify(gameRepository).save(any(Game.class));

        verify(simpMessagingTemplate).convertAndSend(eq("/pub/room/" + roomId), captor.capture());

        InGameUser actual = captor.getValue();
        assertThat(actual.getId()).isEqualTo(userId);
        assertThat(actual.getUsername()).isEqualTo(normalUser.getEmail());
    }

    @Test
    @DisplayName("가득찬 방에 입장할 경우 예외를 던진다.")
    void enterRoomOverMaxPeople() {
        // given
        long roomId = 1L;
        long userId = 2L;
        String email = "sample@test.com";
        String username = email + "_1234";

        Room room = new Room(roomId, 1L, "test room", 8, 8, false, "master@test.com");
        User user = new User(userId, username, email, Role.USER);
        LoginUserRequest loginUserRequest = new LoginUserRequest(userId, email, Role.USER);

        Set<InGameUser> inGameUsers = new HashSet<>();
        inGameUsers.add(new InGameUser(0L, roomId, "master0@test.com", Role.ADMIN, false));
        for (long i = 1; i <= 7; i++) {
            inGameUsers.add(new InGameUser( i + 1, roomId, "user" + i + "@test.com", Role.USER, false));
        }

        Game game = new Game(String.valueOf(roomId), 1L, 1, false, inGameUsers);

        ReentrantLock mockLock = new ReentrantLock();
        roomSubscriptionCount.put(roomId, new AtomicInteger(game.getGameUser().size()));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(gameRepository.findById(String.valueOf(roomId))).willReturn(Optional.of(game));
        given(roomLockManager.getLock(roomId)).willReturn(mockLock);

        ValueOperations<String, Long> longValueOps = mock(ValueOperations.class);
        given(alreadyInGameUserCacheTemplate.opsForValue()).willReturn(longValueOps);

        // when
        // then
        assertThatThrownBy(() -> roomService.enterRoom(roomId, loginUserRequest, ""))
                .isInstanceOf(RoomErrorException.class)
                .hasMessageContaining("정원이 초과되었습니다.");
    }

    @Test
    @DisplayName("이미 같은 방에 입장해 있는 유저가 다시 입장 요청 시 중복 등록 없이 응답한다.")
    void enterRoomUserAlreadyInSameRoom() {
        // given
        long roomId = 1L;
        long userId = 2L;
        String email = "sample@test.com";
        String username = email + "_1234";

        Room room = new Room(roomId, 1L, "test room", 8, 8, false, email);
        User user = new User(userId, username, email, Role.USER);
        LoginUserRequest loginUserRequest = new LoginUserRequest(userId, email, Role.USER);

        InGameUser masterUser = new InGameUser(1L, 1L, "master@sample.com", Role.ADMIN, false);
        InGameUser normalUser = new InGameUser(2L, 1L, username, Role.USER, false);
        Set<InGameUser> inGameUsers = new HashSet<>();
        inGameUsers.add(masterUser);
        inGameUsers.add(normalUser);

        Game game = new Game(String.valueOf(roomId), 1L, 2, false, inGameUsers);

        ReentrantLock mockLock = new ReentrantLock();
        roomSubscriptionCount.put(roomId, new AtomicInteger(2));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(gameRepository.findById(String.valueOf(roomId))).willReturn(Optional.of(game));
        given(roomLockManager.getLock(roomId)).willReturn(mockLock);

        ValueOperations<String, Long> longValueOps = mock(ValueOperations.class);
        given(alreadyInGameUserCacheTemplate.opsForValue()).willReturn(longValueOps);
        given(alreadyInGameUserCacheTemplate.opsForValue().get("userId:" + userId)).willReturn(roomId);

        // when
        RoomEnterResponse response = roomService.enterRoom(roomId, loginUserRequest, "");

        // then
        assertThat(response).isNotNull();
        assertThat(response.roomId()).isEqualTo(roomId);
        assertThat(response.inGameUser().getId()).isEqualTo(userId);
        assertThat(response.participants().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("로그인 하지 않은 유저가 방에 입장할려고 하면 예외를 던진다.")
    void enterRoomNoLoginUser() {
        // given
        long roomId = 1L;

        // when
        // then
        assertThatThrownBy(() -> roomService.enterRoom(roomId, null, ""))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("로그인 해주세요.");
     }

    @Test
    @DisplayName("방 수정을 할 수 있다.")
    void modifyRoomInfo() {
        // given
        Set<InGameUser> set = new HashSet<>();
        String email = "sample@test.com";
        String username= email + "_1234";
        set.add(new InGameUser(1L, 1L, username, Role.ADMIN, false));
        Game game = new Game("1", 1L, 1, false, set);
        Room room = new Room(1L, 1L, "test room", 8, 8, false, email);
        User user = new User(username, email, Role.ADMIN);

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, email, Role.ADMIN);
        RoomModifyRequest request = new RoomModifyRequest("modify room name", 2L, 6, 6);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(gameRepository.findById("1")).willReturn(Optional.of(game));

        // when
        RoomModifyResponse actualResponse = roomService.modifyRoom(request, 1L, loginUserRequest);

        // then
        assertThat(actualResponse)
                .extracting("roomName", "topicId", "maxPeople", "quizCount")
                .contains(request.roomName(), request.topicId(), request.maxPeople(), request.quizCount());
    }

    @Test
    @DisplayName("방 수정 요청을 보낸 유저가 방장이 아니면 예외 처리를 한다.")
    void failModifyRoomNotMatchMaster() {
        // given
        String email = "sample@test.com";
        String masterEmail = "master@test.com";
        String masterUsername = masterEmail + "_1234";

        Room room = new Room(1L, 1L, "test room", 8, 8, false, masterEmail);
        User user = new User(masterUsername, masterEmail, Role.ADMIN);

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, email, Role.USER);
        RoomModifyRequest request = new RoomModifyRequest("modify room name", 2L, 6, 6);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        // then
        assertThatThrownBy(() -> roomService.modifyRoom(request, 1L, loginUserRequest))
                .isInstanceOf(RoomErrorException.class)
                .hasMessage("방 수정에 실패했습니다. 방장이 아닙니다.");
    }

    @Test
    @DisplayName("자신이 속한 방의 수정 요청이 아니면 예외를 보낸다.")
    void failModifyRoomNotMatchRoom() {
        // given
        Room room = new Room(1L, 1L, "test room", 8, 8, false, "master@test.com");
        User user = new User("test User", "master@test.com", Role.ADMIN);

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "anotherMaster@test.com", Role.ADMIN);
        RoomModifyRequest request = new RoomModifyRequest("modify room name", 2L, 6, 6);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        // then
        assertThatThrownBy(() -> roomService.modifyRoom(request, 1L, loginUserRequest))
                .isInstanceOf(RoomErrorException.class)
                .hasMessage("방 수정에 실패했습니다. 현재 방과 다릅니다.");
    }

    @Test
    @DisplayName("수정할 방이 존재하지 않으면 예외를 보낸다.")
    void notFoundRoom() {
        // given
        long roomId = 2;
        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "master@test.com", Role.ADMIN);
        RoomModifyRequest request = new RoomModifyRequest("modify room name", 2L, 6, 6);

        given(roomRepository.findById(2L)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> roomService.modifyRoom(request, 2L, loginUserRequest))
                .isInstanceOf(RoomErrorException.class)
                .hasMessage("존재하지 않는 방입니다. Room ID: " + roomId);
    }

    @Test
    @DisplayName("수정할 방 이름이 제한을 초과하거나 공백이면 예외를 보낸다.")
    void failValidationRoomName() {
        Set<InGameUser> set = new HashSet<>();
        set.add(new InGameUser(1L, 1L, "sample@test.com", Role.ADMIN, false));
        Game game = new Game("1", 1L, 1, false, set);
        Room room = new Room(1L, 1L, "test room", 8, 8, false, "sample@test.com");
        User user = new User("test User", "sample@test.com", Role.ADMIN);

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "sample@test.com", Role.ADMIN);
        RoomModifyRequest request1 = new RoomModifyRequest("modify room name modify room name modify room name", 2L, 6, 6);
        RoomModifyRequest request2 = new RoomModifyRequest("", 2L, 6, 6);
        RoomModifyRequest request3 = new RoomModifyRequest(" ", 2L, 6, 6);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(gameRepository.findById("1")).willReturn(Optional.of(game));

        // when
        // then
        assertThatThrownBy(() -> roomService.modifyRoom(request1, 1L, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 방 제목의 최대 길이는 20자 입니다.");
        assertThatThrownBy(() -> roomService.modifyRoom(request2, 1L, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 방 제목의 최대 길이는 20자 입니다.");
        assertThatThrownBy(() -> roomService.modifyRoom(request3, 1L, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 방 제목의 최대 길이는 20자 입니다.");
    }

    @Test
    @DisplayName("수정할 방 토픽이 없는 토픽이거나 공백이면 예외를 보낸다.")
    void failValidationTopicId() {
        Set<InGameUser> set = new HashSet<>();
        set.add(new InGameUser(1L, 1L, "sample@test.com", Role.ADMIN, false));
        Game game = new Game("1", 1L, 1, false, set);
        Room room = new Room(1L, 1L, "test room", 8, 8, false, "sample@test.com");
        User user = new User("test User", "sample@test.com", Role.ADMIN);

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "sample@test.com", Role.ADMIN);
        RoomModifyRequest request1 = new RoomModifyRequest("modify room name", 5L, 6, 6);
        RoomModifyRequest request2 = new RoomModifyRequest("modify room name", 0L, 6, 6);
        RoomModifyRequest request3 = new RoomModifyRequest("modify room name", null, 6, 6);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(gameRepository.findById("1")).willReturn(Optional.of(game));

        // when
        // then
        assertThatThrownBy(() -> roomService.modifyRoom(request1, 1L, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. topic Id는 1이상 4이하 입니다.");
        assertThatThrownBy(() -> roomService.modifyRoom(request2, 1L, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. topic Id는 1이상 4이하 입니다.");
        assertThatThrownBy(() -> roomService.modifyRoom(request3, 1L, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. topic Id는 1이상 4이하 입니다.");
    }

    @Test
    @DisplayName("수정할 방 퀴즈수가 0이거나 최대를 초과하면 예외를 보낸다.")
    void failValidationQuizCount() {
        Set<InGameUser> set = new HashSet<>();
        set.add(new InGameUser(1L, 1L, "sample@test.com", Role.ADMIN, false));
        Game game = new Game("1", 1L, 1, false, set);
        Room room = new Room(1L, 1L, "test room", 8, 8, false, "sample@test.com");
        User user = new User("test User", "sample@test.com", Role.ADMIN);

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "sample@test.com", Role.ADMIN);
        RoomModifyRequest request1 = new RoomModifyRequest("modify room name", 4L, 6, 11);
        RoomModifyRequest request2 = new RoomModifyRequest("modify room name", 4L, 6, 0);
        RoomModifyRequest request3 = new RoomModifyRequest("modify room name", 4L, 6, null);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(gameRepository.findById("1")).willReturn(Optional.of(game));

        // when
        // then
        assertThatThrownBy(() -> roomService.modifyRoom(request1, 1L, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 최대 문제수는 10문제입니다.");
        assertThatThrownBy(() -> roomService.modifyRoom(request2, 1L, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 최대 문제수는 10문제입니다.");
        assertThatThrownBy(() -> roomService.modifyRoom(request3, 1L, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 최대 문제수는 10문제입니다.");
    }

    @Test
    @DisplayName("수정할 방 최대인원이 현재인원보다 작거나 공백이면 예외를 보낸다.")
    void failValidationMaxPeople1() {
        Set<InGameUser> set = new HashSet<>();
        set.add(new InGameUser(1L, 1L, "sample1@test.com", Role.ADMIN, false));
        set.add(new InGameUser(2L, 1L, "sample2@test.com", Role.USER, false));
        set.add(new InGameUser(3L, 1L, "sample3@test.com", Role.USER, false));
        set.add(new InGameUser(4L, 1L, "sample4@test.com", Role.USER, false));
        set.add(new InGameUser(5L, 1L, "sample5@test.com", Role.USER, false));

        Game game = new Game("1", 1L, 1, false, set);
        Room room = new Room(1L, 1L, "test room", 8, 8, false, "sample@test.com");
        User user = new User("test User", "sample@test.com", Role.ADMIN);

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "sample@test.com", Role.ADMIN);
        RoomModifyRequest request1 = new RoomModifyRequest("modify room name", 4L, 4, 8);
        RoomModifyRequest request2 = new RoomModifyRequest("modify room name", 4L, null, 8);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(gameRepository.findById("1")).willReturn(Optional.of(game));

        // when
        // then
        assertThatThrownBy(() -> roomService.modifyRoom(request1, 1L, loginUserRequest))
                .isInstanceOf(RoomErrorException.class)
                .hasMessage("최대인원은 현재인원보다 작을 수 없습니다.");
        assertThatThrownBy(() -> roomService.modifyRoom(request2, 1L, loginUserRequest))
                .isInstanceOf(RoomErrorException.class)
                .hasMessage("최대인원은 현재인원보다 작을 수 없습니다.");
    }

    @Test
    @DisplayName("수정할 방 최대인원이 최대를 초과하면 예외를 보낸다.")
    void failValidationMaxPeople2() {
        Set<InGameUser> set = new HashSet<>();
        set.add(new InGameUser(1L, 1L, "sample@test.com", Role.ADMIN, false));
        Game game = new Game("1", 1L, 1, false, set);
        Room room = new Room(1L, 1L, "test room", 8, 8, false, "sample@test.com");
        User user = new User("test User", "sample@test.com", Role.ADMIN);

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "sample@test.com", Role.ADMIN);
        RoomModifyRequest request = new RoomModifyRequest("modify room name", 4L, 11, 8);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(gameRepository.findById("1")).willReturn(Optional.of(game));

        // when
        // then
        assertThatThrownBy(() -> roomService.modifyRoom(request, 1L, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 최대 인원은 8명입니다.");
    }
}
