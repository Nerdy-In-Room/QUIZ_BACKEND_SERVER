package com.example.quiz.global.config.cacheConfig.redis;

import com.example.quiz.room.dto.response.RoomResponse;
import com.example.quiz.room.dto.response.ChangeCurrentPeopleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisEventPublisherUnitTest {
    private RedisTemplate<Long, RoomResponse> roomCreatePublishTemplate = mock();
    private RedisTemplate<String, ChangeCurrentPeopleResponse> changeCurrentPeoplePublishTemplate = mock();

    private RedisEventPublisher redisEventPublisher = new RedisEventPublisher(roomCreatePublishTemplate, changeCurrentPeoplePublishTemplate);

    @Test
    @DisplayName("새로운 방이 만들어지면 이벤트 발행한다.")
    void publishCreateEvent() {
        RoomResponse room = new RoomResponse(1L, "test room", 1L, 8, 5, 1);

        redisEventPublisher.publishCreatEvent("create-room-channel", room);

        ArgumentCaptor<RoomResponse> roomResponseArgumentCaptor = ArgumentCaptor.forClass(RoomResponse.class);
        verify(roomCreatePublishTemplate).convertAndSend(eq("create-room-channel"), roomResponseArgumentCaptor.capture());

        RoomResponse actual = roomResponseArgumentCaptor.getValue();
        assertThat(actual.roomId()).isEqualTo(1);
    }

    @Test
    @DisplayName("현재 인원에 변동사항이 생기면 이벤트 발행한다.")
    void publishChangeCurrentPeople_shouldSendToChannel() {
        ChangeCurrentPeopleResponse change = new ChangeCurrentPeopleResponse(1L, 3, System.currentTimeMillis());

        redisEventPublisher.publishChangeCurrentPeople("change-roomList-channel", change);

        verify(changeCurrentPeoplePublishTemplate).convertAndSend("change-roomList-channel", change);
    }
}