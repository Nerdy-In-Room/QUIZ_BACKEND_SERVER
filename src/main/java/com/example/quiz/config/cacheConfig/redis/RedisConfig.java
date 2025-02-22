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
    public RedisTemplate<String, Integer> roomOccupancyCacheTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Integer> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Integer.class));

        return template;
    }

    @Bean
    public RedisTemplate<String, Long> alreadyInGameUserCacheTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));

        return template;
    }

    @Bean
    public RedisTemplate<Long, RoomResponse> roomCreatePublishTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Long, RoomResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new GenericToStringSerializer<>(Long.class));
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

    @Bean
    public RedisTemplate<String, ChangeCurrentOccupancies> changeCurrentOccupanciesPublishTemplate (RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, ChangeCurrentOccupancies> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory redisConnectionFactory,MessageListenerAdapter createRoomAdapter, MessageListenerAdapter changeCurrentOccupanciesAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(createRoomAdapter, new PatternTopic("create-room-channel"));
        container.addMessageListener(changeCurrentOccupanciesAdapter, new PatternTopic("change-roomList-channel"));

        return container;
    }

    @Bean
    public MessageListenerAdapter createRoomAdapter(RedisEventSubscriber subscriber) {

        return new MessageListenerAdapter(subscriber, "createRoomEvent");
    }

    @Bean
    public MessageListenerAdapter changeCurrentOccupanciesAdapter(RedisEventSubscriber subscriber) {

        return new MessageListenerAdapter(subscriber, "changeCurrentOccupancies");
    }

    @Bean
    public RedissonClient redissonClient() {
        Config redissonConfig = new Config();
        redissonConfig.useSingleServer()
                .setAddress("redis://" + redisUrl + ":" + redisPort);

        return Redisson.create(redissonConfig);
    }
}
