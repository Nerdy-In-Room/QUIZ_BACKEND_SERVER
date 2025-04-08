package com.example.quiz.global.config.cacheConfig.redis;

import com.example.quiz.room.dto.response.RoomResponse;
import com.example.quiz.room.dto.response.ChangeCurrentPeopleResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEventSubscriber {
    private final int MAX_QUEUE_SIZE = 10;

    private final SimpMessagingTemplate messagingTemplate;

    private ScheduledFuture<?> scheduledFuture;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final BlockingQueue<ChangeCurrentPeopleResponse> changeCurrentPeopleQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

    public void createRoomEvent(String message) throws JsonProcessingException {
        RoomResponse roomResponse = new ObjectMapper().readValue(message, RoomResponse.class);
        messagingTemplate.convertAndSend("/pub/occupancy", roomResponse);
    }

    public void changeCurrentPeople(String message) {
        try {
            ChangeCurrentPeopleResponse event = new ObjectMapper().readValue(message, ChangeCurrentPeopleResponse.class);

            changeCurrentPeopleQueue.remove(event);
            changeCurrentPeopleQueue.put(event);

            processQueueIfNeeded();
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
        }
    }

    private void processQueueIfNeeded() {
        if (isProcessing.compareAndSet(false, true)) {
            scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
                try {
                    broadcastOccupancy();
                    if (changeCurrentPeopleQueue.isEmpty()) {
                        scheduledFuture.cancel(false);
                        scheduledFuture = null;
                        isProcessing.set(false);
                    }
                } catch (Exception e) {
                    log.error("Error during occupancy broadcasting", e);
                }
            }, 1, 1, TimeUnit.SECONDS);
        }
    }

    private void broadcastOccupancy() {
        List<ChangeCurrentPeopleResponse> response = new ArrayList<>();
        changeCurrentPeopleQueue.drainTo(response);
        messagingTemplate.convertAndSend("/pub/occupancy", response);
    }
}