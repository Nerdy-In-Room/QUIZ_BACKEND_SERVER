package com.example.quiz.global.config.cacheConfig.redis;

import com.example.quiz.room.dto.response.ChangeCurrentPeopleResponse;
import com.example.quiz.room.dto.response.RoomResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class RedisEventSubscriberIntegrationTest {
    @Autowired
    private RedisEventPublisher redisEventPublisher;

    @SpyBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("새로운 방이 생겼을 때 이벤트를 수신하고 브로드캐스트한다.")
    void receiveCreateRoomEventAndBroadcast() {
        // given
        RoomResponse room = new RoomResponse(1L, "test room", 2L, 6, 3, 1);

        // when
        redisEventPublisher.publishCreatEvent("create-room-channel", room);

        // then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        verify(messagingTemplate, timeout(1000))
                .convertAndSend(eq("/pub/occupancy"), captor.capture());

        Object value = captor.getValue();
        assertThat(value).isInstanceOf(RoomResponse.class);

        RoomResponse actual = (RoomResponse) value;
        assertThat(actual).usingRecursiveComparison().isEqualTo(room);
    }

    @Test
    @DisplayName("현재 인원 변경 메시지를 수신하고 브로드캐스트한다")
    void receiveChangeCurrentPeopleAndBroadcast() {
        // given
        ChangeCurrentPeopleResponse event1 = new ChangeCurrentPeopleResponse(1L, 4, System.currentTimeMillis());

        // when
        redisEventPublisher.publishChangeCurrentPeople("change-roomList-channel", event1);

        // then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        verify(messagingTemplate, timeout(1500))
                .convertAndSend(eq("/pub/occupancy"), captor.capture());

        Object value = captor.getValue();
        assertThat(value).isInstanceOf(List.class);

        List<?> list = (List<?>) value;
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).usingRecursiveComparison().isEqualTo(event1);
    }

    @Test
    @DisplayName("현재 인원 변경 메시지 브로드캐스팅 전 같은 roomId를 가진 변경 데이터가 올 경우 최신화 한다.")
    void receiveChangeCurrentPeopleSameRoomIdUpToDateAndBroadcast() {
        // given
        ChangeCurrentPeopleResponse event1 = new ChangeCurrentPeopleResponse(1L, 4, System.currentTimeMillis());
        ChangeCurrentPeopleResponse event2 = new ChangeCurrentPeopleResponse(2L, 4, System.currentTimeMillis());
        ChangeCurrentPeopleResponse event3 = new ChangeCurrentPeopleResponse(1L, 3, System.currentTimeMillis());

        // when
        redisEventPublisher.publishChangeCurrentPeople("change-roomList-channel", event1);
        redisEventPublisher.publishChangeCurrentPeople("change-roomList-channel", event2);
        redisEventPublisher.publishChangeCurrentPeople("change-roomList-channel", event3);

        // then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        verify(messagingTemplate, timeout(1500))
                .convertAndSend(eq("/pub/occupancy"), captor.capture());

        Object value = captor.getValue();
        assertThat(value).isInstanceOf(List.class);

        List<?> queue = (List<?>) value;
        assertThat(queue).hasSize(2);
        assertThat(queue.get(0)).usingRecursiveComparison().isEqualTo(event2);
        assertThat(queue.get(1)).usingRecursiveComparison().isEqualTo(event3);
    }

    @Test
    @DisplayName("1초 이내 10개를 초과한 메시지가 올 경우 10개씩 나눠서 브로드캐스팅한다.")
    void receiveChangeCurrentPeopleTwoTimeBroadcast() {
        // given

        // when
        for (int i = 1; i <= 15; i++) {
            redisEventPublisher.publishChangeCurrentPeople("change-roomList-channel", new ChangeCurrentPeopleResponse(i, 5, System.currentTimeMillis()));
        }

        // then
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        verify(messagingTemplate, timeout(2500).times(2))
                .convertAndSend(eq("/pub/occupancy"), captor.capture());

        List<Object> allValues = captor.getAllValues();
        assertThat(allValues).hasSize(2);

        List<?> firstBroadcast = (List<?>) allValues.get(0);
        assertThat(firstBroadcast).hasSize(10);

        List<?> secondBroadcast = (List<?>) allValues.get(1);
        assertThat(secondBroadcast).hasSize(5);
    }
}
