package com.example.quiz.game.service;

import com.example.quiz.game.dto.response.ResponseQuiz;
import com.example.quiz.game.entity.Game;
import com.example.quiz.game.exception.GameErrorException;
import com.example.quiz.game.model.InGameUser;
import com.example.quiz.game.repository.GameRepository;
import com.example.quiz.global.type.Role;
import com.example.quiz.quiz.dto.request.RequestAnswer;
import com.example.quiz.quiz.dto.response.ResponseReadyGame;
import com.example.quiz.quiz.entity.Quiz;
import com.example.quiz.quiz.repository.QuizRepository;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.repository.RoomRepository;
import com.example.quiz.user.entity.User;
import com.example.quiz.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static com.mongodb.internal.connection.tlschannel.util.Util.assertTrue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Slf4j
class GameServiceTest {
    @Mock
    private GameRepository gameRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoomRepository roomRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @InjectMocks
    private GameService gameService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("빈문자열정답 입력시 예외반환한다.")
    void emptyStringReturnException() {
        // given
        String emptyAnswer = "";
        String roomId = "1";
        Long userId = 1L;

        // when & then
        assertThatThrownBy(() -> {
            gameService.checkAnswer(roomId, new RequestAnswer(userId, emptyAnswer, false));
        })
                .isInstanceOf(GameErrorException.class)
                .hasMessage("정답을 입력해주세요.");
    }

    @Test
    @DisplayName("모든 유저 ready 상태일때 allReady가 true를 반환한다")
    void allReadyReturnTrue() {
        // given
        Long roomId = 1L;
        User user1 = new User("park", "test@email.com", Role.USER);
        User user2 = new User("kim", "user2@email.com", Role.USER);

        InGameUser readyUser1 = new InGameUser(100L, roomId, "test@email.com", Role.USER, false);
        InGameUser readyUser2 = new InGameUser(200L, roomId, "user2@email.com", Role.USER, false);

        Set<InGameUser> users = new HashSet<>(Arrays.asList(readyUser1, readyUser2));
        Game game = new Game(String.valueOf(roomId), roomId, 0, false, users);

        when(userRepository.findById(100L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(200L)).thenReturn(Optional.of(user2));
        when(gameRepository.findById(String.valueOf(roomId))).thenReturn(Optional.of(game));

        // when
        gameService.toggleReadyStatus(String.valueOf(roomId), 100L);
        ArgumentCaptor<ResponseReadyGame> captor1 = ArgumentCaptor.forClass(ResponseReadyGame.class);
        gameService.toggleReadyStatus(String.valueOf(roomId), 200L);
        ArgumentCaptor<ResponseReadyGame> captor2 = ArgumentCaptor.forClass(ResponseReadyGame.class);

        // then
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/pub/room/" + roomId), captor1.capture());
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/pub/room/" + roomId), captor2.capture());

        ResponseReadyGame response1 = captor1.getValue();
        ResponseReadyGame response2 = captor2.getValue();

        assertTrue(response1.readyStatus());
        assertTrue(response2.allReadyStatus());
    }

    @Test
    @DisplayName("문제 중복 출제 검증")
    void validationDuplicateProblem() {
        // given
        String roomId = "1";
        Room room = new Room(1L, 1L, "test", 5, 5, false, "test@gmail.com");
        int numberOfQuestions = 5;
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(quizRepository.findAllByTopicId(1L)).thenReturn(List.of(
                new Quiz(1L,1L,"problem1", "answer", "des"),
                new Quiz(2L,1L,"problem2", "answer", "des"),
                new Quiz(3L,1L,"problem3", "answer", "des"),
                new Quiz(4L,1L,"problem4", "answer", "des"),
                new Quiz(5L,1L,"problem5", "answer", "des"),
                new Quiz(6L,1L,"problem6", "answer", "des"),
                new Quiz(7L,1L,"problem7", "answer", "des"),
                new Quiz(8L,1L,"problem8", "answer", "des"),
                new Quiz(9L,1L,"problem9", "answer", "des"),
                new Quiz(10L,1L,"problem10", "answer", "des")
        ));

        // when
        Set<String> quizzes = new HashSet<>();
        for (int i = 0; i < numberOfQuestions; i++) {
            gameService.sendQuiz(String.valueOf(room.getRoomId()));
            ArgumentCaptor<ResponseQuiz> captor = ArgumentCaptor.forClass(ResponseQuiz.class);
            verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/pub/quiz/" + roomId), captor.capture());
            quizzes.add(captor.getValue().problem());
        }

        assertEquals(numberOfQuestions, quizzes.size());
    }
}