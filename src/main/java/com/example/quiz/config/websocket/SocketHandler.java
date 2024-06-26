package com.example.quiz.config.websocket;

import com.example.quiz.dto.response.CurrentOccupancy;
import com.example.quiz.service.RoomService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class SocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SocketHandler.class);
    private final RoomService roomService;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<Long> currentPageRooms = parseMessage(message);

        List<CurrentOccupancy> occupancyList = roomService.getCurrentOccupancy(currentPageRooms);
        String response = mapper.writeValueAsString(occupancyList);
        log.info("현재인원 : {}", response);

        session.sendMessage(new TextMessage(response));
    }

    private List<Long> parseMessage(TextMessage message) throws Exception {
        String payload = message.getPayload();
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<List<Long>> typeReference = new TypeReference<>() {};

        return mapper.readValue(payload, typeReference);
    }
}
