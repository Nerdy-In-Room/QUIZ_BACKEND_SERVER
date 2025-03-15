package com.example.quiz.service;

import com.example.quiz.dto.User.LoginUserRequest;
import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.response.RoomResponse;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.user.User;
import com.example.quiz.enums.Role;
import com.example.quiz.mapper.RoomMapper;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.RoomRepository;
import com.example.quiz.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomProducerServiceTest {
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

    @InjectMocks
    private RoomProducerService roomProducerService;

    @Mock
    private RLock rLock;

    @Mock
    private RoomMapper roomMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateRoom_Success() throws Exception {
        String uuid = "test-uuid";
        RoomCreateRequest roomCreateRequest = new RoomCreateRequest("Test Room", 1L, 10, 5, uuid);
        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@example.com", "USER");

        Optional<User> user = Optional.of(new User(1L, "test@example.com_3002860612", "test@example.com", Role.USER));
        Room room = new Room(1L, 1L, roomCreateRequest.roomName(), 8, 10, false, "test@example.com");
        RoomResponse expectedResponse = new RoomResponse(1L, "Test Room", 1L, 8, 10, 1);

        ValueOperations<String, RoomResponse> valueOperationsMock = mock(ValueOperations.class);

        // Mock 설정
        when(redissonClient.getLock("room:create:" + uuid)).thenReturn(rLock); // RLock Mock
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true); // Lock 획득 성공 설정
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(roomCreateCacheTemplate.opsForValue()).thenReturn(valueOperationsMock); // opsForValue 반환값 설정
        when(valueOperationsMock.get(uuid)).thenReturn(null); // Redis에서 값 없음을 가정
        when(userRepository.findById(1L)).thenReturn(user);
        when(roomRepository.save(any(Room.class))).thenReturn(room); // Room 저장 Mock
        when(roomMapper.RoomToRoomResponse(any(Room.class))).thenReturn(expectedResponse); // Room -> RoomResponse 변환 Mock


        // 테스트 대상 메서드 호출
        RoomResponse actualResponse = roomProducerService.createRoom(roomCreateRequest, loginUserRequest);

        // 결과 검증
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);

        // Lock 및 Redis Cache 동작 검증
        verify(rLock).unlock(); // Lock 해제 검증
        verify(valueOperationsMock).set(eq(uuid), eq(expectedResponse), eq(1L), eq(TimeUnit.MINUTES)); // Cache 삽입 검증
    }

    @Test
    void testCreateRoom_LockNotAcquired() throws Exception {
        String uuid = "test-uuid";
        RoomCreateRequest roomCreateRequest = new RoomCreateRequest("Test Room", 1L, 10, 5, uuid);
        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@example.com", "USER");

        when(redissonClient.getLock("room:create:" + uuid)).thenReturn(rLock);
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(false);

        RoomResponse actualResponse = roomProducerService.createRoom(roomCreateRequest, loginUserRequest);

        assertNull(actualResponse);

        verify(rLock, never()).unlock();
    }

    @Test
    void testCreateRoom_CacheHit() throws Exception {
        String uuid = "test-uuid";
        RoomCreateRequest roomCreateRequest = new RoomCreateRequest("Test Room", 1L, 10, 5, uuid);
        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@example.com", "USER");

        RoomResponse cachedResponse = new RoomResponse(1L, "Test Room", 1L, 10, 5, 1);

        ValueOperations<String, RoomResponse> valueOperationsMock = mock(ValueOperations.class);

        when(redissonClient.getLock("room:create:" + uuid)).thenReturn(rLock);
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(roomCreateCacheTemplate.opsForValue()).thenReturn(valueOperationsMock); // opsForValue 반환값 설정
        when(valueOperationsMock.get(uuid)).thenReturn(cachedResponse);

        RoomResponse actualResponse = roomProducerService.createRoom(roomCreateRequest, loginUserRequest);

        assertNotNull(actualResponse);
        assertEquals(cachedResponse, actualResponse);

        verify(rLock).unlock();
        verify(roomRepository, never()).save(any(Room.class));
    }
}