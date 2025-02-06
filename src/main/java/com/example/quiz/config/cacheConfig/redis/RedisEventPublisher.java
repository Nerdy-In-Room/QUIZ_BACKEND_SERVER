package com.example.quiz.config.cacheConfig.redis;

import com.example.quiz.dto.room.ChangeCurrentOccupancies;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisEventPublisher {
    private final RedisTemplate<String, ChangeCurrentOccupancies> changeCurrentOccupanciesRedisTemplate;

    public void publishChangeCurrentOccupancies(String channel, ChangeCurrentOccupancies changeCurrentOccupancies) {
        changeCurrentOccupanciesRedisTemplate.convertAndSend(channel, changeCurrentOccupancies);
    }
}
