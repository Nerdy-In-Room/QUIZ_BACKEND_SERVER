package com.example.quiz.config.cacheConfig.redis;

import com.example.quiz.dto.room.ChangeCurrentOccupancies;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

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
    private final BlockingQueue<ChangeCurrentOccupancies> changeCurrentOccupanciesQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

    public void handleMessage(String message) {
        try {
            ChangeCurrentOccupancies event = new ObjectMapper().readValue(message, ChangeCurrentOccupancies.class);

            changeCurrentOccupanciesQueue.remove(event);
            changeCurrentOccupanciesQueue.put(event);

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
                    if (changeCurrentOccupanciesQueue.isEmpty()) {
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
        messagingTemplate.convertAndSend("/pub/occupancy", changeCurrentOccupanciesQueue);
        changeCurrentOccupanciesQueue.clear();
    }
}
