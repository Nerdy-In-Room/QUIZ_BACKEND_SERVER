package com.example.quiz.global.config.stompConfig;

import com.example.quiz.global.type.Role;
import com.example.quiz.game.entity.Game;
import com.example.quiz.game.exception.GameErrorCode;
import com.example.quiz.game.exception.GameErrorException;
import com.example.quiz.game.model.InGameUser;
import com.example.quiz.game.repository.GameRepository;
import com.example.quiz.global.config.cacheConfig.redis.RedisEventPublisher;
import com.example.quiz.global.exception.general.GeneralErrorCode;
import com.example.quiz.global.exception.general.GeneralErrorException;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.exception.RoomErrorCode;
import com.example.quiz.room.exception.RoomErrorException;
import com.example.quiz.room.model.ChangeCurrentPeople;
import com.example.quiz.room.repository.RoomRepository;
import com.example.quiz.user.dto.request.LoginUserRequest;
import com.example.quiz.user.entity.User;
import com.example.quiz.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final RoomRepository roomRepository;
    private final StompHeaderAccessorWrapper headerAccessorService;

    private final String ROOM_ID_PREFIX = "roomId:";
    private final String USER_ID_PREFIX = "userId:";
    private final String REDIS_PUBLISH_CHANNEL = "change-roomList-channel";


    private final RedisEventPublisher redisEventPublisher;
    private final Map<Long, AtomicInteger> roomSubscriptionCount;
    private final RedisTemplate<String, Integer> roomOccupancyCacheTemplate;
    private final RedisTemplate<String, Long> alreadyInGameUserCacheTemplate;

    @EventListener
    @Transactional
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = headerAccessorService.wrap(event);

        LoginUserRequest loginUserRequest = extractLoginUser(accessor);
        Long roomId = removeUserFromRoomMapping(loginUserRequest.userId());
        Game game = findGameByRoomId(roomId);

        removeUserFromGame(game, loginUserRequest.userId(), roomId);
        updateRoomSubscriptionCount(roomId);
    }

    private LoginUserRequest extractLoginUser(StompHeaderAccessor accessor) {
        LoginUserRequest loginUserRequest = (LoginUserRequest) accessor.getSessionAttributes().get("loginUser");

        if (loginUserRequest == null) {
            throw new GeneralErrorException(GeneralErrorCode.USER_NOT_FOUND);
        }

        return loginUserRequest;
    }

    private Long removeUserFromRoomMapping(Long userId) {
        Long roomId = alreadyInGameUserCacheTemplate.opsForValue().get(USER_ID_PREFIX + userId);

        if (roomId == null) {
            throw new GameErrorException(GameErrorCode.USER_NOT_IN_GAME);
        }

        boolean delete = alreadyInGameUserCacheTemplate.delete(USER_ID_PREFIX + userId);

        if (!delete) {
            throw new RoomErrorException(RoomErrorCode.NOT_FOUND_ROOM, "Room ID: " + roomId);
        }

        return roomId;
    }

    private Game findGameByRoomId(Long roomId) {
        return gameRepository.findById(String.valueOf(roomId))
                .orElseThrow(() -> new RoomErrorException(RoomErrorCode.NOT_FOUND_ROOM, "Room ID: " + roomId));
    }

    private void removeUserFromGame(Game game, Long userId, Long roomId) throws IllegalArgumentException {
        InGameUser inGameUser = findUser(userId, roomId);
        game.getGameUser().remove(inGameUser);
        gameRepository.save(game);
    }

    public void updateRoomSubscriptionCount(Long roomId) {
        AtomicInteger count = roomSubscriptionCount.get(roomId);

        if (count == null) {

            return;
        }

        int currentCount = count.updateAndGet(current -> {
            if (current < 1) {
                throw new RuntimeException("방 인원 음수가 될 수 없습니다.: " + roomId);
            }

            roomOccupancyCacheTemplate.opsForValue().decrement(ROOM_ID_PREFIX + roomId);

            return current - 1;
        });

        if (currentCount <= 0) {
            cleanUpEmptyRoom(roomId);
        }

        redisEventPublisher.publishChangeCurrentOccupancies(REDIS_PUBLISH_CHANNEL, new ChangeCurrentPeople(roomId, currentCount));
    }

    private void cleanUpEmptyRoom(Long roomId) {
        roomSubscriptionCount.remove(roomId);
        roomRepository.findById(roomId).ifPresent(Room::removeStatus);
        gameRepository.removeById(String.valueOf(roomId));
        roomOccupancyCacheTemplate.delete(ROOM_ID_PREFIX + roomId);
    }

    private InGameUser findUser(long userId, long roomId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralErrorException(GeneralErrorCode.USER_NOT_FOUND));

        return new InGameUser(user.getId(), roomId, user.getEmail(), Role.USER, false);
    }
}