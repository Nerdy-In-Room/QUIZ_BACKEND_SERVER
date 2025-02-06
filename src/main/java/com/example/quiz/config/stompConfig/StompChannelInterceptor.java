package com.example.quiz.config.stompConfig;

import com.example.quiz.dto.User.LoginUserRequest;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class StompChannelInterceptor implements ChannelInterceptor {
    private final AtomicInteger clientToServerTraffic = new AtomicInteger(0);
    private final AtomicInteger serverToClientTraffic = new AtomicInteger(0);

    @Autowired
    public StompChannelInterceptor(MeterRegistry meterRegistry) {
        Gauge.builder("stomp.client_to_server.bytes", clientToServerTraffic, AtomicInteger::get)
                .description("Current traffic from client to server")
                .register(meterRegistry);

        Gauge.builder("stomp.server_to_client.bytes", serverToClientTraffic, AtomicInteger::get)
                .description("Current traffic from server to client")
                .register(meterRegistry);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        LoginUserRequest loginUserRequest = null;

        try {
            loginUserRequest = extractLoginUser(accessor);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        int payloadSize = 0;
        Object payload = message.getPayload();

        if (payload instanceof byte[]) {
            payloadSize = ((byte[]) payload).length;
        } else if (payload instanceof String) {
            payloadSize = ((String) payload).getBytes().length;
        } else if (payload instanceof Serializable) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(payload);
                oos.flush();
                payloadSize = bos.size();
            } catch (IOException e) {
                log.error("Failed to calculate payload size", e);
            }
        }

        if (accessor.getSessionId() != null && accessor.getCommand() != null) {
            log.info("Client to Server Command: {}, Payload Size: {}, login user: {}", accessor.getCommand(), payloadSize, loginUserRequest.email());
            clientToServerTraffic.addAndGet(payloadSize);
        } else {
            log.info("Server to Client, Payload Size: {}", payloadSize);
            serverToClientTraffic.addAndGet(payloadSize);
        }

        return message;
    }

    private LoginUserRequest extractLoginUser(StompHeaderAccessor event) throws IllegalAccessException {
        LoginUserRequest loginUserRequest = (LoginUserRequest) event.getSessionAttributes().get("loginUser");

        if (loginUserRequest == null) {
            throw new IllegalAccessException("Login user is null in session attributes");
        }

        return loginUserRequest;
    }
}
