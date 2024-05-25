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
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class GameService {

    private final GameRepository gameRepository;
    private final QuizRepository quizRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public ResponseMessage gameStatusService(String roomId, Long userId) {

        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setUserId(userId);

        // 개발자의 역활에 따른 로직을 나누어야 한다.
        // 유저의 역할이 admin이면 방에 들어온 유저의 수와 게임에 status가 true인 사람의 갯수 -1를 비교하여
        // 똑같으면 게임의 상태를 true로 변경하고 admin의 status 도 변경한다.

        Game game = gameRepository.findById(roomId).orElseThrow(IllegalAccessError::new);
        // 게임 데이터에서 유저들의 데이터만 받아오는 코드
        User findUser = userRepository.findById(userId).orElseThrow();

        // requestUserInfo 에서 userid를 가지고 와서 user 정보를 찾음

        // 해당 유저의 역할이 유저일 경우
        if (findUser.getRole().equals(Role.USER)) {
            // 유저의 정보를 수정함
            findUser.setReadyStatus(!findUser.isReadyStatus());

            gameRepository.save(game);
            responseMessage.setCheckedAnswer(findUser.isReadyStatus());
            // 해당 유저의 역할이 방장일 경우
        } else if (findUser.getRole().equals(Role.ADMIN)) {
            long count = game.getGameUser().stream().filter(User::isReadyStatus).count();
            if (count == game.getCurrentParticipantsNo() - 1) {
                findUser.setReadyStatus(!findUser.isReadyStatus());
                // 유저의 정보를 수정함
                game.setIsGaming(true);
                gameRepository.save(game);
                responseMessage.setCheckedAnswer(true);
            }
        }
        return responseMessage;
    }

    @Transactional
    public ResponseQuiz sendQuiz(String roomId, RequestUserInfoAnswer userInfoAnswer) {
        // TODO: 게임의 첫문제를 전송하는 코드 작성
        // 문제를 제출할 때, 문제의 id와 문제 내용을 전달한
        // 사용자는 유저의 아이디, 문제 id, 정답을 같이 전달함
        // 서버는 문제 id를 사용하여 사용자의 답과 정답이 맞는 확인하고 전달함
        ResponseQuiz responseQuize = new ResponseQuiz();
        List<Long> questionList = userInfoAnswer.getQuestionList();

        // TODO: 옳바른 예외를 발생시키게 찾아보자
        Room room = roomRepository.findById(Long.parseLong(roomId)).orElseThrow(null);
        Long topicId = room.getTopicId();
        List<Quiz> allByTopicId = quizRepository.findAllByTopicId(topicId);
        int size = allByTopicId.size();
        Random random = new Random();

        if (userInfoAnswer.getQuestionList().isEmpty()) {
            int pickNumber = random.nextInt(size);
            Quiz quiz = allByTopicId.get(pickNumber);
            questionList.add(quiz.getId());
            responseQuize.setQuizId(quiz.getId());
            responseQuize.setQuestion(quiz.getQuestion());
        } else {
            while (true) {
                int pickNumber = random.nextInt(size);
                Quiz quiz = allByTopicId.get(pickNumber);
                questionList.contains(quiz.getId());
                if (!questionList.contains(quiz.getId())) {
                    questionList.add(quiz.getId());

                    responseQuize.setQuizId(quiz.getId());
                    responseQuize.setQuestion(quiz.getQuestion());
                    break;
                }
            }
        }
        return responseQuize;
    }

    public ResponseMessage checkAnswer(String id, RequestAnswer requestAnswer) {

        Quiz quiz = quizRepository.findById(requestAnswer.getQuizId()).get();

        return ResponseMessage.builder()
                .userId(requestAnswer.getUserId())
                .isCheckedAnswer(quiz.getAnswer().equals(requestAnswer.getAnswer()))
                .build();
    }
}
