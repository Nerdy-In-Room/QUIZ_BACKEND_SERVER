package com.example.quiz.global.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class RoomLockManager {
    private final ConcurrentHashMap<Long, ReentrantLock> roomLocks = new ConcurrentHashMap<>();

    public ReentrantLock getLock(Long roomId) {
        return roomLocks.computeIfAbsent(roomId,id -> new ReentrantLock());
    }
}
