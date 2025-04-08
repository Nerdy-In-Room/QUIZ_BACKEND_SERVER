package com.example.quiz.room.repository;

import com.example.quiz.room.entity.Room;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RoomRepositoryTest {
    @Autowired
    private RoomRepository roomRepository;

    @Test
    @DisplayName("remove status가 false인 방을 검색한다.")
    void findAllByRemoveStatus() {
        // given
        int index = 0;
        int pageSize = 10;
        Room room1 = createRoom(1L, "test room1", 8, 8, false, "master1@sample.com");
        Room room2 = createRoom(2L, "test room2", 7, 7, false, "master2@sample.com");
        Room room3 = createRoom(3L, "test room3", 6, 6, false, "master3@sample.com");
        Room room4 = createRoom(4L, "test room4", 5, 5, true, "master4@sample.com");
        Room room5 = createRoom(3L, "test room5", 4, 4, false, "master5@sample.com");

        roomRepository.saveAll(List.of(room1, room2, room3, room4, room5));
        Pageable pageable = PageRequest.of(index, pageSize, Sort.by("roomId").descending());

        // when
        Page<Room> list = roomRepository.findAllByRemoveStatus(false, pageable);

        // then
        assertThat(list.getContent()).hasSize(4)
                .extracting("topicId", "roomName", "maxPeople", "quizCount", "removeStatus", "masterEmail")
                .containsExactly(
                        tuple(3L, "test room5", 4, 4, false, "master5@sample.com"),
                        tuple(3L, "test room3", 6, 6, false, "master3@sample.com"),
                        tuple(2L, "test room2", 7, 7, false, "master2@sample.com"),
                        tuple(1L, "test room1", 8, 8, false, "master1@sample.com")
                );
    }

    private Room createRoom(Long topicId, String roomName, int maxPeople, int quizCount, boolean removeStatus, String masterEmail) {
        return Room.builder()
                .topicId(topicId)
                .roomName(roomName)
                .maxPeople(maxPeople)
                .quizCount(quizCount)
                .removeStatus(removeStatus)
                .masterEmail(masterEmail)
                .build();
    }
}