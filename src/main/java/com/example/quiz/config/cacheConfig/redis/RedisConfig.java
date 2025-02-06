package com.example.quiz.config.cacheConfig.redis;

import com.example.quiz.dto.room.ChangeCurrentOccupancies;
import com.example.quiz.dto.room.response.RoomResponse;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String redisUrl;
    @Value("${spring.data.redis.port}")
    private String redisPort;

    @Bean
    public RedisTemplate<String, RoomResponse> roomCreateCacheTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, RoomResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

    @Bean
    public RedisTemplate<Long, Integer> roomOccupancyCacheTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Long, Integer> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new GenericToStringSerializer<>(Long.class));
        template.setValueSerializer(new GenericToStringSerializer<>(Integer.class));

        return template;
    }

    @Bean
    public RedisTemplate<String, ChangeCurrentOccupancies> changeCurrentOccupanciesRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, ChangeCurrentOccupancies> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory redisConnectionFactory, MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListenerAdapter, new PatternTopic("change-occupancies-channel"));

        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(RedisEventSubscriber subscriber) {

        return new MessageListenerAdapter(subscriber, "handleMessage");
    }

    @Bean
    public RedissonClient redissonClient() {
        log.info(redisUrl);
        log.info(redisPort);
        Config redissonConfig = new Config();
        redissonConfig.useSingleServer()
                .setAddress("redis://" + redisUrl + ":" + redisPort);

        return Redisson.create(redissonConfig);
    }
}
