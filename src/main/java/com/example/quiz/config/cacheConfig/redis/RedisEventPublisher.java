package com.example.quiz.config.cacheConfig.redis;

import com.example.quiz.dto.room.ChangeCurrentOccupancies;
import com.example.quiz.dto.room.response.RoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisEventPublisher {
    private final RedisTemplate<Long, RoomResponse> roomCreatePublishTemplate;
    private final RedisTemplate<String, ChangeCurrentOccupancies> changeCurrentOccupanciesPublishTemplate;

    public void publishCreatEvent(String channel, RoomResponse roomResponse) {
        roomCreatePublishTemplate.convertAndSend(channel, roomResponse);
    }

    public void publishChangeCurrentOccupancies(String channel, ChangeCurrentOccupancies changeCurrentOccupancies) {
        changeCurrentOccupanciesPublishTemplate.convertAndSend(channel, changeCurrentOccupancies);
    }
}
