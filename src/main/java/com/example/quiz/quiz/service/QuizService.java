package com.example.quiz.quiz.service;

import com.example.quiz.game.model.InGameUser;
import com.example.quiz.global.exception.general.GeneralErrorCode;
import com.example.quiz.global.exception.general.GeneralErrorException;
import com.example.quiz.global.type.Role;
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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    public QuizRoomEnterResponse enterGameRoom(long roomId, LoginUserRequest loginUserRequest) {
        User user = findUser(loginUserRequest);
        Room room = findRoomById(roomId);
        InGameUser inGameUser = findInGameUser(roomId, loginUserRequest);

        return RoomMapper.INSTANCE.RoomToQuizRoomEnterResponse(inGameUser, user, room);
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
