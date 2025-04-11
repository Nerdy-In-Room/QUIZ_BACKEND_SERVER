package com.example.quiz.global.config.cacheConfig.redis;

import com.example.quiz.room.dto.response.RoomResponse;
import com.example.quiz.room.dto.response.ChangeCurrentPeopleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class RedisEventPublisherIntegrationTest {

    @Autowired
    private RedisEventPublisher redisEventPublisher;

    @SpyBean
    private RedisEventSubscriber redisEventSubscriber;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("방 생성 이벤트가 Redis Pub/Sub을 통해 전달된다.")
    void publishCreateRoomEvent() throws Exception {
        // given
        RoomResponse room = new RoomResponse(1L, "test room", 2L, 6, 3, 1);

        // when
        redisEventPublisher.publishCreatEvent("create-room-channel", room);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        verify(redisEventSubscriber, timeout(1000)).createRoomEvent(captor.capture());

        String actualJson = captor.getValue();
        RoomResponse actualRoom = objectMapper.readValue(actualJson, RoomResponse.class);

        assertThat(actualRoom).usingRecursiveComparison().isEqualTo(room);
    }

    @Test
    @DisplayName("현재 인원 변경 이벤트가 Redis Pub/Sub을 통해 전달된다.")
    void publishChangeCurrentPeople() throws Exception {
        // given
        ChangeCurrentPeopleResponse event = new ChangeCurrentPeopleResponse(1L, 4, 1L);

        // when
        redisEventPublisher.publishChangeCurrentPeople("change-roomList-channel", event);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        verify(redisEventSubscriber, timeout(1000)).changeCurrentPeople(captor.capture());

        String actualJson = captor.getValue();
        ChangeCurrentPeopleResponse actualRoom = objectMapper.readValue(actualJson, ChangeCurrentPeopleResponse.class);

        assertThat(actualRoom).usingRecursiveComparison().isEqualTo(event);
    }
}