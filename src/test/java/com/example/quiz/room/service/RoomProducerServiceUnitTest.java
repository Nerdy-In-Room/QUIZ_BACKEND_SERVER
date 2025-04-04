package com.example.quiz.room.service;

import com.example.quiz.game.repository.GameRepository;
import com.example.quiz.global.exception.general.GeneralErrorException;
import com.example.quiz.global.type.Role;
import com.example.quiz.room.dto.request.RoomCreateRequest;
import com.example.quiz.room.dto.response.RoomResponse;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.mapper.RoomMapper;
import com.example.quiz.room.repository.RoomRepository;
import com.example.quiz.user.dto.request.LoginUserRequest;
import com.example.quiz.user.entity.User;
import com.example.quiz.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class RoomProducerServiceUnitTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RedisTemplate<String, RoomResponse> roomCreateCacheTemplate;
    @Mock
    private RLock rLock;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    RoomProducerService roomProducerService;

    @Test
    @DisplayName("방 생성 검증이 모두 통과일 경우 테스트한다.")
    void createRoomGoodCase() throws InterruptedException {
        // given
        String uuid = "test-uuid";
        User user = new User(1L, "test@example.com_3002860612", "test@example.com", Role.USER);

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@example.com", "USER");
        RoomCreateRequest roomCreateRequest = new RoomCreateRequest("Test Room", 1L, 8, 10, uuid);
        Room room = new Room(1L, roomCreateRequest.topicId(), roomCreateRequest.roomName(), roomCreateRequest.maxPeople(), roomCreateRequest.quizCount(), false, "test@example.com");

        RoomResponse expectedResponse = new RoomResponse(1L, "Test Room", 1L, 8, 10, 1);

        ValueOperations<String, RoomResponse> valueOperationsMock = mock(ValueOperations.class);

        given(redissonClient.getLock("room:create:" + uuid)).willReturn(rLock);
        given(rLock.tryLock(5, 10, TimeUnit.SECONDS)).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(roomCreateCacheTemplate.opsForValue()).willReturn(valueOperationsMock);
        given(valueOperationsMock.get(uuid)).willReturn(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(roomRepository.save(any(Room.class))).willReturn(room);

        // when
        RoomResponse actualResponse = roomProducerService.createRoom(roomCreateRequest, loginUserRequest);

        // then
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("잘못된 topicID가 입력으로 들어왔을 때 예외 테스트한다.")
    void createRoomWrongTopicId() throws InterruptedException {
        // given
        String uuid = "test-uuid";

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@example.com", "USER");
        RoomCreateRequest roomCreateRequest1 = new RoomCreateRequest("Test Room", 0L, 8, 5, uuid);
        RoomCreateRequest roomCreateRequest2 = new RoomCreateRequest("Test Room", 5L, 8, 5, uuid);

        ValueOperations<String, RoomResponse> valueOperationsMock = mock(ValueOperations.class);

        given(redissonClient.getLock("room:create:" + uuid)).willReturn(rLock);
        given(rLock.tryLock(5, 10, TimeUnit.SECONDS)).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(roomCreateCacheTemplate.opsForValue()).willReturn(valueOperationsMock);
        given(valueOperationsMock.get(uuid)).willReturn(null);

        // when
        // then
        assertThatThrownBy(() -> roomProducerService.createRoom(roomCreateRequest1, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. topic Id는 1이상 4이하 입니다.");
        assertThatThrownBy(() -> roomProducerService.createRoom(roomCreateRequest2, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. topic Id는 1이상 4이하 입니다.");
    }

    @Test
    @DisplayName("잘못된 방 이름가 입력으로 들어왔을 때 예외 테스트한다.")
    void createRoomWrongRoomName() throws InterruptedException {
        // given
        String uuid = "test-uuid";

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@example.com", "USER");
        RoomCreateRequest roomCreateRequest1 = new RoomCreateRequest("", 4L, 8, 5, uuid);
        RoomCreateRequest roomCreateRequest2 = new RoomCreateRequest(" ", 4L, 8, 5, uuid);

        ValueOperations<String, RoomResponse> valueOperationsMock = mock(ValueOperations.class);

        given(redissonClient.getLock("room:create:" + uuid)).willReturn(rLock);
        given(rLock.tryLock(5, 10, TimeUnit.SECONDS)).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(roomCreateCacheTemplate.opsForValue()).willReturn(valueOperationsMock);
        given(valueOperationsMock.get(uuid)).willReturn(null);

        // when
        // then
        assertThatThrownBy(() -> roomProducerService.createRoom(roomCreateRequest1, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 방 제목의 최대 길이는 20자 입니다.");
        assertThatThrownBy(() -> roomProducerService.createRoom(roomCreateRequest2, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 방 제목의 최대 길이는 20자 입니다.");
    }

    @Test
    @DisplayName("잘못된 최대인원으로 들어왔을 때 예외 테스트한다.")
    void createRoomWrongRoomNameLess() throws InterruptedException {
        // given
        String uuid = "test-uuid";

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@example.com", "USER");
        RoomCreateRequest roomCreateRequest1 = new RoomCreateRequest("test room", 4L, 0, 5, uuid);
        RoomCreateRequest roomCreateRequest2 = new RoomCreateRequest("test room", 4L, 9, 5, uuid);

        ValueOperations<String, RoomResponse> valueOperationsMock = mock(ValueOperations.class);

        given(redissonClient.getLock("room:create:" + uuid)).willReturn(rLock);
        given(rLock.tryLock(5, 10, TimeUnit.SECONDS)).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(roomCreateCacheTemplate.opsForValue()).willReturn(valueOperationsMock);
        given(valueOperationsMock.get(uuid)).willReturn(null);

        // when
        // then
        assertThatThrownBy(() -> roomProducerService.createRoom(roomCreateRequest1, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 최대 인원은 8명입니다.");
        assertThatThrownBy(() -> roomProducerService.createRoom(roomCreateRequest2, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 최대 인원은 8명입니다.");
    }

    @Test
    @DisplayName("잘못된 퀴즈 수로 들어왔을 때 예외 테스트한다.")
    void createRoomWrongRoomQuizCount() throws InterruptedException {
        // given
        String uuid = "test-uuid";

        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@example.com", "USER");
        RoomCreateRequest roomCreateRequest1 = new RoomCreateRequest("test room", 4L, 1, 0, uuid);
        RoomCreateRequest roomCreateRequest2 = new RoomCreateRequest("test room", 4L, 8, 11, uuid);

        ValueOperations<String, RoomResponse> valueOperationsMock = mock(ValueOperations.class);

        given(redissonClient.getLock("room:create:" + uuid)).willReturn(rLock);
        given(rLock.tryLock(5, 10, TimeUnit.SECONDS)).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(roomCreateCacheTemplate.opsForValue()).willReturn(valueOperationsMock);
        given(valueOperationsMock.get(uuid)).willReturn(null);

        // when
        // then
        assertThatThrownBy(() -> roomProducerService.createRoom(roomCreateRequest1, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 최대 문제수는 10문제입니다.");
        assertThatThrownBy(() -> roomProducerService.createRoom(roomCreateRequest2, loginUserRequest))
                .isInstanceOf(GeneralErrorException.class)
                .hasMessage("잘못된 입력입니다. 최대 문제수는 10문제입니다.");
    }
}
