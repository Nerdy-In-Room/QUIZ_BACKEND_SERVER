package com.example.quiz.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
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

    @Bean
    public ConcurrentHashMap<Long, List<Long>> roomQuizMap() {

        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentHashMap<Long, Map<Long,Long>> currentInGameScore() {

        return new ConcurrentHashMap<>();
    }

    @Bean
    public ConcurrentHashMap<Long, Integer> remainQuizMap() {

        return new ConcurrentHashMap<>();
    }
}
