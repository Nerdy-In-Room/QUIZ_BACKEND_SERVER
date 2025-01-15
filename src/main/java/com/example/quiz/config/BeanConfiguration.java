package com.example.quiz.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@RequiredArgsConstructor
public class BeanConfiguration {

    @Bean
    public ConcurrentHashMap<Long, Long> alreadyInGameUser() {

        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentHashMap<Long, AtomicInteger> roomSubscriptionCount() {

        return new ConcurrentHashMap<>();
    }
}
