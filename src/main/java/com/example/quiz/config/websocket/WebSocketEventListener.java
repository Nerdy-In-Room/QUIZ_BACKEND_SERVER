package com.example.quiz.config.websocket;

import com.example.quiz.entity.Room;
import com.example.quiz.repository.RoomRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private static final String ROOM_PREFIX = "/room/";
    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final ConcurrentHashMap<String, Integer> roomSubscriptionCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionDestinationMap = new ConcurrentHashMap<>();
    private final RoomRepository roomRepository;

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) throws IllegalAccessException {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = extractDestinationFromEvent(accessor);

        if (isRoomDestination(destination)) {
            if (sessionDestinationMap.containsKey(sessionId)) {
                throw new IllegalAccessException();
            }

            if (roomSubscriptionCount.get(destination) >= 8) {
                throw new IllegalAccessException();
            }

            sessionDestinationMap.put(sessionId, destination);
            roomSubscriptionCount.merge(destination, 1, Integer::sum);
        }
    }

    @EventListener
    @Transactional
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = sessionDestinationMap.remove(sessionId);

        if (isRoomDestination(destination)) {
            roomSubscriptionCount.merge(destination, -1, Integer::sum);
        }

        if (roomSubscriptionCount.getOrDefault(destination, 0) == 0) {
            Optional<Room> room = roomRepository.findById(getRoomId(destination));

            room.ifPresent(Room::removeStatus);
        }
    }

    private String extractDestinationFromEvent(StompHeaderAccessor accessor) {
        return accessor.getDestination();
    }

    private boolean isRoomDestination(String destination) {
        return destination != null && destination.startsWith(ROOM_PREFIX);
    }

    public int getSubscriptionCount(long roomId) {
        return roomSubscriptionCount.getOrDefault("/room/" + roomId, 0);
    }

    public long getRoomId(String destination) {
        String[] url = destination.split("/");

        return Long.parseLong(url[2]);
    }
}