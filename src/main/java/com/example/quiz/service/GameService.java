package com.example.quiz.service;

import com.example.quiz.dto.request.RequestAnswer;
import com.example.quiz.dto.request.RequestUserInfoAnswer;
import com.example.quiz.dto.response.ResponseMessage;
import com.example.quiz.dto.response.ResponseQuiz;
import com.example.quiz.entity.Game;
import com.example.quiz.entity.Quiz;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.User;
import com.example.quiz.enums.Role;
import com.example.quiz.repository.GameRepository;
import com.example.quiz.repository.QuizRepository;
import com.example.quiz.repository.RoomRepository;
import com.example.quiz.repository.UserRepository;
import com.example.quiz.vo.InGameUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class GameService {

    private final GameRepository gameRepository;
    private final QuizRepository quizRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ResponseMessage toggleReadyStatus(String roomId, Long userId) {
        // User, Game 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Game game = gameRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        // 현재 로그인한 InGameUser 반환
        Set<InGameUser> inGameUserSet = game.getGameUser();
        // TODO Optional 반환값으로 변환
        InGameUser currentUser = findUser(inGameUserSet, userId);

        // 준비상태 토글
        toggle(game, currentUser);
        // User 준비상태 따라서 DTO 반환
        return handleReadyStatus(user, currentUser, inGameUserSet);
    }

    private InGameUser findUser(Set<InGameUser> inGameUserSet, long userId) {
        for(InGameUser inGameUser : inGameUserSet) {
            if(inGameUser.getId() == userId) {
                return inGameUser;
            }
        }
        return null;
    }

    private void toggle(Game game, InGameUser inGameUser) {
        // 준비상태 변화 및 게임방 최신화
        inGameUser.changeReadyStatus(!inGameUser.isReadyStatus());
        game.getGameUser().add(inGameUser);
        gameRepository.save(game);
    }
    // User
    private ResponseMessage handleReadyStatus(User user, InGameUser inGameUser, Set<InGameUser> inGameUserSet) {
        if(isAllReady(inGameUserSet)) {
            return new ResponseMessage(user.getId(), user.getEmail(), user.getRole(), inGameUser.isReadyStatus(), true);
        }
        else {
            return new ResponseMessage(user.getId(), user.getEmail(), user.getRole(), inGameUser.isReadyStatus(), false);
        }
    }

    // User 인 사람이 모두 Ready 인지 판단
    private boolean isAllReady(Set<InGameUser> inGameUserSet) {
        for(InGameUser inGameUser : inGameUserSet) {
            // Admin 통과
            if(!isUser(inGameUser)) {
                continue;
            }
            if(!inGameUser.isReadyStatus()) {
                return false;
            }
        }
        return true;
    }

    private boolean isUser(InGameUser inGameUser) {
        return inGameUser.getRole() == Role.USER;
    }

    @Transactional
    public ResponseQuiz sendQuiz(String roomId, RequestUserInfoAnswer userInfoAnswer) {
        // TODO: 게임의 첫문제를 전송하는 코드 작성
        // 문제를 제출할 때, 문제의 id와 문제 내용을 전달한
        // 사용자는 유저의 아이디, 문제 id, 정답을 같이 전달함
        // 서버는 문제 id를 사용하여 사용자의 답과 정답이 맞는 확인하고 전달함

        List<Long> questionList = userInfoAnswer.questionList();

        // TODO: 옳바른 예외를 발생시키게 찾아보자
        Room room = roomRepository.findById(Long.parseLong(roomId)).orElseThrow(IllegalAccessError::new);
        Long topicId = room.getTopicId();
        List<Quiz> allByTopicId = quizRepository.findAllByTopicId(topicId);
        int size = allByTopicId.size();
        Random random = new Random();

        if (userInfoAnswer.questionList().isEmpty()) {
            int pickNumber = random.nextInt(size);
            Quiz quiz = allByTopicId.get(pickNumber);
            questionList.add(quiz.getId());
            return new ResponseQuiz(userInfoAnswer.userId(), "songkc123@naver.com", quiz.getTopicId(), 3);
        } else {
            while (true) {
                int pickNumber = random.nextInt(size);
                Quiz quiz = allByTopicId.get(pickNumber);

                if (!questionList.contains(quiz.getId())) {
                    questionList.add(quiz.getId());

                    return new ResponseQuiz(userInfoAnswer.userId(), "songkc123@naver.com", quiz.getTopicId(), 3);
                }
            }
        }
    }
    // TODO ResponseQuiz 수정
    public ResponseQuiz checkAnswer(String id, RequestAnswer requestAnswer) {

        Quiz quiz = quizRepository.findById(requestAnswer.quizId()).get();

        return new ResponseQuiz(requestAnswer.userId(), "songkc123@naver.com", quiz.getTopicId(), 3);
    }
}
