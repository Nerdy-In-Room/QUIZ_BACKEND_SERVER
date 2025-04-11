package com.example.quiz.global.config.cacheConfig.redis;

import com.example.quiz.room.dto.response.ChangeCurrentPeopleResponse;
import com.example.quiz.room.dto.response.RoomResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisEventSubscriberUnitTest {
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RedisEventSubscriber subscriber;

    @Test
    @DisplayName("방이 생성되면 클라이언트에 브로드캐스팅한다.")
    void createRoomEvent() throws Exception {
        RoomResponse room = new RoomResponse(1L, "test room", 1L, 8, 5, 1);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(room);

        subscriber.createRoomEvent(json);

        verify(messagingTemplate).convertAndSend("/pub/occupancy", room);
    }

    @Test
    @DisplayName("현재 인원이 변동되면 클라이언트에 브로드캐스팅한다.")
    void changeCurrentPeopleEvent() throws Exception {
        ChangeCurrentPeopleResponse change = new ChangeCurrentPeopleResponse(1L, 4, System.currentTimeMillis());
        String json = new ObjectMapper().writeValueAsString(change);

        subscriber.changeCurrentPeople(json);
        Thread.sleep(1500);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        verify(messagingTemplate, atLeastOnce()).convertAndSend((eq("/pub/occupancy")), captor.capture());
        Object sentPayload = captor.getValue();
        assertThat(sentPayload).isInstanceOf(List.class);

        List<?> list = (List<?>) sentPayload;
        assertThat(list).extracting("roomId").containsExactly(1L);
    }
}