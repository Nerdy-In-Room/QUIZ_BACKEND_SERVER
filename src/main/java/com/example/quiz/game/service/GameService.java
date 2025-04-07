package com.example.quiz.game.service;

import com.example.quiz.global.type.Role;
import com.example.quiz.game.dto.response.ResponseCheckQuiz;
import com.example.quiz.game.dto.response.ResponseQuiz;
import com.example.quiz.game.entity.Game;
import com.example.quiz.game.exception.GameErrorCode;
import com.example.quiz.game.exception.GameErrorException;
import com.example.quiz.game.model.InGameUser;
import com.example.quiz.game.repository.GameRepository;
import com.example.quiz.game.validation.GameValidation;
import com.example.quiz.global.exception.general.GeneralErrorCode;
import com.example.quiz.global.exception.general.GeneralErrorException;
import com.example.quiz.quiz.dto.request.RequestAnswer;
import com.example.quiz.quiz.dto.request.RequestRemainQuiz;
import com.example.quiz.quiz.dto.response.ResponseReadyGame;
import com.example.quiz.quiz.dto.response.ResponseStartGame;
import com.example.quiz.quiz.entity.Quiz;
import com.example.quiz.quiz.repository.QuizRepository;
import com.example.quiz.room.entity.Room;
import com.example.quiz.room.exception.RoomErrorCode;
import com.example.quiz.room.exception.RoomErrorException;
import com.example.quiz.room.repository.RoomRepository;
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
public class GameService {
    private static final Map<Long, Integer> remainQuizMap = new ConcurrentHashMap<>();

    private final GameRepository gameRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void toggleReadyStatus(String roomId, Long userId) {
        // User, Game 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralErrorException(GeneralErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다."));
        Game game = gameRepository.findById(roomId)
                .orElseThrow(() -> new RoomErrorException(RoomErrorCode.NOT_FOUND_ROOM, "게임방을 찾을 수 없습니다."));

        // 현재 로그인한 InGameUser 반환
        Set<InGameUser> inGameUserSet = game.getGameUser();
        InGameUser currentUser = findUser(inGameUserSet, userId);

        // 준비상태 토글
        toggle(game, currentUser);
        // User 준비상태 따라서 DTO 반환
        messagingTemplate.convertAndSend("/pub/room/" + roomId, handleReadyStatus(user, currentUser, inGameUserSet));
    }

    private InGameUser findUser(Set<InGameUser> inGameUserSet, long userId) {
        for (InGameUser inGameUser : inGameUserSet) {
            if (inGameUser.getId() == userId) {
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
    private ResponseReadyGame handleReadyStatus(User user, InGameUser inGameUser, Set<InGameUser> inGameUserSet) {
        if (isAllReady(inGameUserSet)) {
            return new ResponseReadyGame(user.getId(), user.getEmail(), user.getRole(), inGameUser.isReadyStatus(), true);
        } else {
            return new ResponseReadyGame(user.getId(), user.getEmail(), user.getRole(), inGameUser.isReadyStatus(), false);
        }
    }

    // User 인 사람이 모두 Ready 인지 판단
    private boolean isAllReady(Set<InGameUser> inGameUserSet) {
        for (InGameUser inGameUser : inGameUserSet) {
            // Admin 통과
            if (!isUser(inGameUser)) {
                continue;
            }
            if (!inGameUser.isReadyStatus()) {
                return false;
            }
        }
        return true;
    }

    private boolean isUser(InGameUser inGameUser) {
        return inGameUser.getRole() == Role.USER;
    }

    @Transactional
    public void startGame(String roomId, RequestRemainQuiz requestRemainQuiz) {
        Room room = roomRepository.findById(Long.parseLong(roomId)).orElseThrow(() -> new RoomErrorException(RoomErrorCode.NOT_FOUND_ROOM, "Room ID: " + roomId));
        room.changeQuizCount(requestRemainQuiz.remainQuiz());

        messagingTemplate.convertAndSend("/pub/room/" + roomId, new ResponseStartGame(room.getQuizCount()));
    }
}
