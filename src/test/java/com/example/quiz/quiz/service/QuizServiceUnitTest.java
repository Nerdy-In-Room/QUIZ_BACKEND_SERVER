package com.example.quiz.quiz.service;

import com.example.quiz.game.dto.response.ResponseCheckQuiz;
import com.example.quiz.game.dto.response.ResponseQuiz;
import com.example.quiz.game.entity.Game;
import com.example.quiz.game.repository.GameRepository;
import com.example.quiz.global.type.Role;
import com.example.quiz.quiz.dto.request.RequestAnswer;
import com.example.quiz.quiz.entity.Quiz;
import com.example.quiz.quiz.repository.QuizRepository;
import com.example.quiz.room.dto.response.QuizRoomEnterResponse;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.repository.RoomRepository;
import com.example.quiz.user.dto.request.LoginUserRequest;
import com.example.quiz.user.entity.User;
import com.example.quiz.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final Map<Long, List<Long>> roomQuizMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Map<Long, Long>> currentInGameScore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> remainQuizMap = new ConcurrentHashMap<>();
    QuizService quizService;

    @BeforeEach
    void setup() {
        quizService = new QuizService(
                roomQuizMap, currentInGameScore, remainQuizMap,
                userRepository, roomRepository, gameRepository,
                quizRepository, messagingTemplate);
    }

    @Test
    @DisplayName("인게임 입장에 성공한다.")
    void testEnterQuizRoom_Success() {
        // given
        long roomId = 1L;
        LoginUserRequest loginUserRequest = new LoginUserRequest(1L, "test@naver.com", Role.USER);
        User user = new User("park", "test@email.com", Role.USER);
        Room room = new Room(1L, 1L, "test", 5, 5, false, "test@gmail.com");

        // 목 객체의 리턴 값 설정
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(roomRepository.findById(anyLong())).thenReturn(Optional.of(room));

        // when
        QuizRoomEnterResponse response = quizService.enterQuizRoom(roomId, loginUserRequest);

        // then
        assertNotNull(response);
        // response 내용 검증
        verify(roomRepository, times(1)).findById(roomId);
    }

    @Test
    @DisplayName("퀴즈 문제 정답 설명이 올바르게 초기화 된다.")
    void testStartQuiz_QuizInitialization() {
        // Given
        String roomId = "1";
        Long roomIdLong = 1L;

        // 테스트용 Room 객체 생성
        Room room = new Room(1L, 1L, "test", 5, 5, false, "test@gmail.com");

        // RoomRepository가 올바른 Room을 반환하도록 설정
        when(roomRepository.findById(roomIdLong)).thenReturn(Optional.of(room));
        // 테스트용 Quiz 생성 (selectRandomQuiz 메소드에서 사용할 문제)
        Quiz quiz = new Quiz(1L, 1L, "What is Java?", "A programming language", "A popular programming language");

        // quizRepository에서 topicId로 조회 시, 위 Quiz 한 개를 반환하도록 설정
        List<Quiz> quizList = Collections.singletonList(quiz);
        when(quizRepository.findAllByTopicId(room.getTopicId())).thenReturn(quizList);

        // messagingTemplate은 실제 전송 없이 호출 여부만 확인
        doNothing().when(messagingTemplate).convertAndSend(anyString(), Optional.ofNullable(any()));
        remainQuizMap.put(roomIdLong, room.getQuizCount());

        // When
        quizService.startQuiz(roomId);

        // Then
        // messagingTemplate의 convertAndSend가 올바른 경로와 ResponseQuiz 객체로 호출되었는지 검증
        ArgumentCaptor<ResponseQuiz> responseQuizCaptor = ArgumentCaptor.forClass(ResponseQuiz.class);
        verify(messagingTemplate).convertAndSend(eq("/pub/quiz/" + roomId), responseQuizCaptor.capture());
        ResponseQuiz responseQuiz = responseQuizCaptor.getValue();
        assertNotNull(responseQuiz, "ResponseQuiz 객체는 null이 아니어야 합니다.");
        assertEquals("What is Java?", responseQuiz.problem(), "문제 내용이 올바르지 않습니다.");
        assertEquals("A programming language", responseQuiz.correctAnswer(), "정답이 올바르지 않습니다.");
        assertEquals("A popular programming language", responseQuiz.description(), "설명이 올바르지 않습니다.");

        // remainQuizMap의 퀴즈 개수가 1 감소했는지도 확인 (5 -> 4)
        assertEquals(4, remainQuizMap.get(roomIdLong), "remainQuizMap이 올바르게 감소되지 않았습니다.");
    }

    @Test
    @DisplayName("인게임과 방과 참가자 정보가 올바르게 초기화 된다.")
    void testInitializeGameOnQuizEnd() {
        // Given
        String roomId = "1";
        Long roomIdLong = 1L;

        // 마지막 라운드를 시뮬레이션 하기 위해, Room의 quizCount를 1로 설정합니다.
        // (startQuiz 호출 시 merge 로직에서 remainQuizMap의 값이 1 - 1 = 0이 됩니다.)
        Room room = new Room(1L, 1L, "test", 1, 1, false, "test@gmail.com");
        // spy를 이용하여 removeStatus 메소드 호출 여부를 검증합니다.
        Room roomSpy = spy(room);

        when(roomRepository.findById(roomIdLong)).thenReturn(Optional.of(roomSpy));

        // remainQuizMap에 초기 퀴즈 개수를 설정 (테스트용 헬퍼 메소드 사용)
        remainQuizMap.put(roomIdLong, room.getQuizCount());

        // QuizRepository에서 topicId로 조회 시, 임의의 Quiz 한 건을 반환하게 설정
        Quiz quiz = new Quiz(2L, 1L, "Question?", "Answer", "Description");
        List<Quiz> quizList = Collections.singletonList(quiz);
        when(quizRepository.findAllByTopicId(room.getTopicId())).thenReturn(quizList);

        // 메시지 전송은 실제 동작하지 않도록 stub 처리
        doNothing().when(messagingTemplate).convertAndSend(anyString(), Optional.ofNullable(any()));

        // When
        // startQuiz 호출 시 remainQuizMap 값이 1 → 0으로 감소하면서 initializeGameOnQuizEnd가 실행됩니다.
        quizService.startQuiz(roomId);

        // Then
        // Room의 상태 변경: 마지막 라운드이므로 Room.removeStatus()가 호출되어야 함.
        verify(roomSpy, times(1)).removeStatus();

        // Game 엔티티 생성 및 저장: gameRepository.save()가 호출되었는지 검증.
        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository, times(1)).save(gameCaptor.capture());
        Game savedGame = gameCaptor.getValue();

        // 생성된 Game 객체의 필드 검증
        assertEquals(String.valueOf(roomIdLong), savedGame.getId(), "Game ID가 올바르지 않습니다.");
        assertEquals(roomIdLong, savedGame.getRoomId(), "roomId가 올바르지 않습니다.");
        assertNotNull(savedGame.getCurrentParticipantsNo(), "플레이어 Set이 null이면 안됩니다.");
        assertTrue(savedGame.getGameUser().isEmpty(), "플레이어 Set은 초기에는 비어 있어야 합니다.");
    }

    @Test
    @DisplayName("마지막 라운드에 정답 플래그가 올바르게 초기화 된다.")
    void checkAnswer_correctAnswer_notFinalQuiz() {
        // Given
        String roomId = "1";
        Long roomIdLong = 1L;
        // 테스트용 사용자, 방, 퀴즈 생성
        User user = new User("john", "john@example.com", Role.USER);
        Room room = new Room(1L, 1L, "roomTest", 5, 5, false, "room@example.com");
        Quiz quiz = new Quiz(10L, 1L, "Capital of France?", "Paris", "France's capital is Paris");

        // 목 리턴 값 설정
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomIdLong)).thenReturn(Optional.of(room));
        // roomQuizMap의 최신 퀴즈 id 설정 (정답이 검증될 수 있도록)
        // (내부적으로 correctQuizId() 메소드가 마지막 Quiz id를 사용하므로)
        roomQuizMap.putIfAbsent(room.getRoomId(), List.of(quiz.getQuizId()));
        when(quizRepository.findById(quiz.getQuizId())).thenReturn(Optional.of(quiz));

        // 정답 입력 (정답이 "Paris"로 일치)
        RequestAnswer requestAnswer = new RequestAnswer(1L, "Paris", false);

        // stub: messagingTemplate 호출에 대해 아무것도 하지 않음
        doNothing().when(messagingTemplate).convertAndSend(anyString(), Optional.ofNullable(any()));

        // When
        quizService.checkAnswer(roomId, requestAnswer);

        // Then
        // 올바른 응답 메시지가 전달되었는지 확인 (true 플래그와 finalQuiz false)
        ArgumentCaptor<ResponseCheckQuiz> captor = ArgumentCaptor.forClass(ResponseCheckQuiz.class);
        verify(messagingTemplate).convertAndSend(eq("/pub/quiz/" + roomId), captor.capture());
        ResponseCheckQuiz response = captor.getValue();

        assertTrue(response.currentResult(), "정답인 경우 정답 플래그가 true여야 합니다.");
        assertFalse(response.finalResult(), "finalQuiz 플래그는 false여야 합니다.");
    }

    @Test
    @DisplayName("오답일때 정답 플래그와 최종 승리자 리스트가 올바르게 할당된다.")
    void checkAnswer_incorrectAnswer() {
        // Given
        String roomId = "1";
        Long roomIdLong = 1L;
        User user = new User("john", "john@example.com", Role.USER);
        Room room = new Room(1L, 1L, "roomTest", 5, 5, false, "room@example.com");
        Quiz quiz = new Quiz(10L, 1L, "Capital of France?", "Paris", "France's capital is Paris");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomIdLong)).thenReturn(Optional.of(room));
        when(quizRepository.findById(quiz.getQuizId())).thenReturn(Optional.of(quiz));
        roomQuizMap.putIfAbsent(room.getRoomId(), List.of(quiz.getQuizId()));

        // 오답 입력 (정답 "Paris" 대신 다른 답)
        RequestAnswer requestAnswer = new RequestAnswer(1L, "London", false);

        doNothing().when(messagingTemplate).convertAndSend(anyString(), Optional.ofNullable(any()));

        // When
        quizService.checkAnswer(roomId, requestAnswer);

        // Then
        ArgumentCaptor<ResponseCheckQuiz> captor = ArgumentCaptor.forClass(ResponseCheckQuiz.class);
        verify(messagingTemplate).convertAndSend(eq("/pub/quiz/" + roomId), captor.capture());
        ResponseCheckQuiz response = captor.getValue();

        assertFalse(response.currentResult(), "오답인 경우 정답 플래그가 false여야 합니다.");
        // 오답일 때 최종 승자 리스트는 null이어야 합니다.
        assertNull(response.finalWinners(), "오답인 경우 최종 승자 리스트는 null이어야 합니다.");
    }
}