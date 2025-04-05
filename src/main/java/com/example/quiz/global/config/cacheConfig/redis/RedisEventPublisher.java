package com.example.quiz.global.config.cacheConfig.redis;

import com.example.quiz.room.model.ChangeCurrentPeople;
import com.example.quiz.room.dto.response.RoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisEventPublisher {
    private final RedisTemplate<Long, RoomResponse> roomCreatePublishTemplate;
    private final RedisTemplate<String, ChangeCurrentPeople> changeCurrentPeoplePublishTemplate;

    public void publishCreatEvent(String channel, RoomResponse roomResponse) {
        roomCreatePublishTemplate.convertAndSend(channel, roomResponse);
    }

    public void publishChangeCurrentPeople(String channel, ChangeCurrentPeople changeCurrentPeople) {
        changeCurrentPeoplePublishTemplate.convertAndSend(channel, changeCurrentPeople);
    }
}
