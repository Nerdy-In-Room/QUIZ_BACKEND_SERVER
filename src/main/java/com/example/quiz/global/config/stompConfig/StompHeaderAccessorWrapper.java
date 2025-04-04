package com.example.quiz.global.config.stompConfig;

import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class StompHeaderAccessorWrapper {
    public StompHeaderAccessor wrap(SessionSubscribeEvent event) {

        return StompHeaderAccessor.wrap(event.getMessage());
    }

    public StompHeaderAccessor wrap(SessionUnsubscribeEvent event) {

        return StompHeaderAccessor.wrap(event.getMessage());
    }
}
