package com.example.quiz.mapper;

import com.example.quiz.dto.response.QuizRoomEnterResponse;
import com.example.quiz.dto.response.ResponseQuiz;
import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.response.RoomEnterResponse;
import com.example.quiz.dto.room.response.RoomListResponse;
import com.example.quiz.dto.room.response.RoomResponse;
import com.example.quiz.entity.Room;
import com.example.quiz.entity.User;
import com.example.quiz.vo.InGameUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Set;

@Mapper
public interface RoomMapper {
    RoomMapper INSTANCE = Mappers.getMapper(RoomMapper.class);

    @Mapping(target = "removeStatus", constant = "false")
    Room RoomCreateRequestToRoom(RoomCreateRequest request, String masterEmail);

    @Mapping(target = "currentPeople", constant = "1")
    RoomResponse RoomToRoomResponse(Room room);

    RoomListResponse RoomToRoomListResponse(Room room, Integer currentPeople);

    @Mapping(target = "roomId", source = "room.roomId")
    @Mapping(target = "participants", source = "participants")
    @Mapping(target = "inGameUser", source = "inGameUser")
    RoomEnterResponse RoomToRoomEnterResponse(Room room, InGameUser inGameUser, Set<InGameUser> participants);

    @Mapping(target = "quizId", source = "room.topicId")
    @Mapping(target = "userId", source = "inGameUser.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "role", source = "inGameUser.role")
    QuizRoomEnterResponse RoomToQuizRoomEnterResponse(InGameUser inGameUser, User user, Room room);
}
