package com.example.quiz.quiz.service;

import com.example.quiz.game.dto.response.ResponseCheckQuiz;
import com.example.quiz.game.dto.response.ResponseQuiz;
import com.example.quiz.game.entity.Game;
import com.example.quiz.game.exception.GameErrorCode;
import com.example.quiz.game.exception.GameErrorException;
import com.example.quiz.game.model.InGameUser;
import com.example.quiz.game.repository.GameRepository;
import com.example.quiz.game.validation.GameValidation;
import com.example.quiz.global.config.cacheConfig.redis.RedisConfig;
import com.example.quiz.global.exception.general.GeneralErrorCode;
import com.example.quiz.global.exception.general.GeneralErrorException;
import com.example.quiz.global.type.Role;
import com.example.quiz.quiz.dto.request.RequestAnswer;
import com.example.quiz.quiz.entity.Quiz;
import com.example.quiz.quiz.repository.QuizRepository;
import com.example.quiz.room.dto.response.QuizRoomEnterResponse;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.exception.RoomErrorCode;
import com.example.quiz.room.exception.RoomErrorException;
import com.example.quiz.room.mapper.RoomMapper;
import com.example.quiz.room.repository.RoomRepository;
import com.example.quiz.user.dto.request.LoginUserRequest;
import com.example.quiz.user.entity.User;
import com.example.quiz.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {
    private final Map<Long, List<Long>> roomQuizMap;
    private final Map<Long, Map<Long, Long>> currentInGameScore;
    private final Map<Long, Integer> remainQuizMap;

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;
    private final QuizRepository quizRepository;

    private final SimpMessagingTemplate messagingTemplate;

    public QuizRoomEnterResponse enterQuizRoom(long roomId, LoginUserRequest loginUserRequest) {
        User user = findUser(loginUserRequest);
        Room room = findRoomById(roomId);
        InGameUser inGameUser = findInGameUser(roomId, loginUserRequest);
        if (remainQuizMap.isEmpty()) {
            remainQuizMap.put(Long.parseLong(String.valueOf(room.getRoomId())), room.getQuizCount());
        }

        return RoomMapper.INSTANCE.RoomToQuizRoomEnterResponse(inGameUser, user, room);
    }

    @Transactional
    public void startQuiz(String roomId) {
        Room room = roomRepository.findById(Long.valueOf(roomId)).orElseThrow(() -> new RoomErrorException(RoomErrorCode.NOT_FOUND_ROOM, "Room ID: " + roomId));
        Quiz quiz = selectRandomQuiz(Long.parseLong(roomId), room.getTopicId());

        remainQuizMap.merge(Long.parseLong(roomId), 1, (oldValue, newValue) -> oldValue - 1);
        initializeGameOnQuizEnd(Long.parseLong(roomId));
        log.info("remainQuiz : {}", remainQuizMap.get(Long.parseLong(roomId)));
        messagingTemplate.convertAndSend("/pub/quiz/" + roomId, new ResponseQuiz(quiz.getProblem(), quiz.getCorrectAnswer(), quiz.getDescription()));
    }

    // 마지막 라운드 시작시 게임방 생성
    private void initializeGameOnQuizEnd(Long roomId) {
        if (remainQuizMap.get(roomId) == 0) {
            roomRepository.findById(roomId).ifPresent(Room::removeStatus);
            Game game = new Game(String.valueOf(roomId), roomId, 0, false, new HashSet<>());
            gameRepository.save(game);
        }
    }

    // topic Id 맞게 중복되지 않는 Quiz 반환
    public Quiz selectRandomQuiz(Long roomId, Long topicId) {
        roomQuizMap.putIfAbsent(roomId, new ArrayList<>());
        List<Long> usedQuizIds = roomQuizMap.get(roomId);

        List<Quiz> allQuizzes = quizRepository.findAllByTopicId(topicId);
        List<Quiz> availableQuizzes = quizRepository.findAllByTopicId(topicId).stream()
                .filter(quiz -> !usedQuizIds.contains(quiz.getQuizId()))
                .toList();

        // 사용 가능한 문제가 없으면 모든 문제를 다시 사용 가능하도록 초기화
        if (availableQuizzes.isEmpty()) {
            log.info("문제를 다 풀었습니다. 문제집을 초기화 합니다.");
            usedQuizIds.clear();
            availableQuizzes = allQuizzes;
        }

        int randomIndex = new Random().nextInt(availableQuizzes.size());
        Quiz selectedQuiz = availableQuizzes.get(randomIndex);

        usedQuizIds.add(selectedQuiz.getQuizId());
        return selectedQuiz;
    }

    public void checkAnswer(String id, RequestAnswer requestAnswer) {
        GameValidation.validateAnswer(requestAnswer.answer());

        User user = userRepository.findById(requestAnswer.userId()).orElseThrow(() -> new GeneralErrorException(GeneralErrorCode.USER_NOT_FOUND));
        Room room = roomRepository.findById(Long.valueOf(id)).orElseThrow(() -> new RoomErrorException(RoomErrorCode.NOT_FOUND_ROOM, "Room ID: " + id));
        Long correctQuizId = correctQuizId(roomQuizMap.get(room.getRoomId()));
        Quiz quiz = quizRepository.findById(correctQuizId).orElseThrow(() -> new GameErrorException(GameErrorCode.QUIZ_NOT_FOUND));
        boolean isRight = check(requestAnswer.answer(), quiz);

        // 정답이 맞으면 정답 반환. 오답이면 null 반환.
        if (isRight) {
            increaseScore(user.getId(), room.getRoomId());
            // final winner 반환
            List<String> finalWinners = findFinalWinners(room.getRoomId());
            if (requestAnswer.finalQuiz()) {
                removeInGameInfo(room.getRoomId());
                messagingTemplate.convertAndSend("/pub/quiz/" + id
                        , new ResponseCheckQuiz(user.getEmail(), true, true, finalWinners, quiz.getCorrectAnswer(), quiz.getDescription()));
            } else {
                messagingTemplate.convertAndSend("/pub/quiz/" + id
                        , new ResponseCheckQuiz(user.getEmail(), true, false, finalWinners, quiz.getCorrectAnswer(), quiz.getDescription()));
            }
        } else {
            messagingTemplate.convertAndSend("/pub/quiz/" + id
                    , new ResponseCheckQuiz(user.getEmail(), false, false, null, quiz.getCorrectAnswer(), quiz.getDescription()));
        }
    }

    private boolean check(String answer, Quiz quiz) {
        return quiz.getCorrectAnswer().equals(answer);
    }

    // 게임이 끝난 후 인게임 정보 삭제
    private void removeInGameInfo(Long roomId) {
        currentInGameScore.remove(roomId);
        roomQuizMap.remove(roomId);
    }

    private Long correctQuizId(List<Long> usedQuizIds) {
        return usedQuizIds.stream()
                .skip(usedQuizIds.size() - 1)
                .findFirst()
                .orElse(-1L);
    }

    // 방이 없으면 추가하고, 점수 카운팅을 한다
    private void increaseScore(Long userId, Long roomId) {
        currentInGameScore.putIfAbsent(roomId, new HashMap<>());
        Map<Long, Long> score = currentInGameScore.get(roomId);
        score.put(userId, score.getOrDefault(userId, 0L) + 1L);
    }

    // 최종 우승자 반환
    private List<String> findFinalWinners(Long roomId) {
        Map<Long, Long> score = currentInGameScore.get(roomId);
        if (score == null || score.isEmpty()) {
            return Collections.emptyList();
        }

        // 1) 최대 점수 찾기
        Long maxScore = score.values().stream()
                .max(Long::compare)
                .orElse(Long.MIN_VALUE);

        // 2) 최대 점수를 가진 userId들 필터 & 이메일 변환
        return score.entrySet().stream()
                // 최대 점수를 가진 엔트리만 추출
                .filter(e -> e.getValue().equals(maxScore))
                // userId 추출
                .map(Map.Entry::getKey)
                // userId -> email 변환 (UserRepository 예시)
                .map(userId -> userRepository.findById(userId)
                        .map(User::getEmail)
                        .orElse("존재하지 않는 유저"))  // 존재하지 않는 사용자 처리
                .toList();
    }

    private User findUser(LoginUserRequest loginUserRequest) {
        return userRepository.findById(loginUserRequest.userId()).orElseThrow(() -> new GeneralErrorException(GeneralErrorCode.USER_NOT_FOUND));
    }

    private Room findRoomById(long roomId) {
        return roomRepository.findById(roomId).orElseThrow(() -> new RoomErrorException(RoomErrorCode.NOT_FOUND_ROOM, "Room ID: " + roomId));
    }

    private InGameUser findInGameUser(long roomId, LoginUserRequest loginUserRequest) {
        User user = findUser(loginUserRequest);

        if (loginUserRequest.role().equals(Role.ADMIN)) {
            return new InGameUser(loginUserRequest.userId(), roomId, user.getEmail(), Role.ADMIN, false);
        }

        return new InGameUser(loginUserRequest.userId(), roomId, user.getEmail(), Role.USER, false);
    }
}
