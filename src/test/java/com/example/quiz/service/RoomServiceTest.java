package com.example.quiz.service;

import com.example.quiz.user.dto.request.LoginUserRequest;
import com.example.quiz.room.dto.request.RoomCreateRequest;
import com.example.quiz.user.entity.User;
import com.example.quiz.global.type.Role;
import com.example.quiz.user.repository.UserRepository;
import com.example.quiz.room.service.RoomProducerService;
import com.example.quiz.room.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class RoomServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private Map<Long, Long> alreadyInGameUser;
    @Autowired
    private Map<Long, AtomicInteger> roomSubscriptionCount;
    @Autowired
    private RoomProducerService roomProducerService;
    @Autowired
    private UserRepository userRepository;

    private final int TEST_THREAD = 100;

    @Container
    private static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:latest"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("init.sql");

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Test
    public void subscriptionTest() throws ExecutionException, InterruptedException, IllegalAccessException {
        AtomicLong roomNumber = new AtomicLong(1);
        AtomicLong userNumber = new AtomicLong(1);

        for (long i = 1; i <= 100; i++) {
            userRepository.save(new User(i, "user" + i, "test@test.com", Role.USER));
        }

        roomSubscriptionCount.put(1L, new AtomicInteger(1));
        roomSubscriptionCount.put(2L, new AtomicInteger(1));

        roomProducerService.createRoom(new RoomCreateRequest("room1", 1L, 8, 8, "1"), new LoginUserRequest(1L, "email", "USER"));
        roomProducerService.createRoom(new RoomCreateRequest("room2", 1L, 8, 8, "2"), new LoginUserRequest(2L, "email", "USER"));

        List<CompletableFuture<Void>> list = new ArrayList<>();

        CyclicBarrier barrier = new CyclicBarrier(100);

        for (long i = 1; i <= TEST_THREAD; i++) {
            long now = userNumber.getAndIncrement();
            list.add(CompletableFuture.runAsync(
                    () -> {
                        try {
                            barrier.await();

                            roomService.enterRoom(roomNumber.getAndIncrement() % 2 + 1, new LoginUserRequest(now, "email", "USER"), "");
                        } catch (RuntimeException e) {
                            log.info("user: {}, {}", now, e.getMessage());
                        } catch (BrokenBarrierException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
            ));
        }

        CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).get();

        Assertions.assertEquals(8, roomSubscriptionCount.get(1L).get());
        Assertions.assertEquals(8, roomSubscriptionCount.get(2L).get());
    }
}