package com.example.quiz.room.mapper;

import com.example.quiz.game.model.InGameUser;
import com.example.quiz.room.dto.request.RoomCreateRequest;
import com.example.quiz.room.dto.response.QuizRoomEnterResponse;
import com.example.quiz.room.dto.response.RoomEnterResponse;
import com.example.quiz.room.dto.response.RoomListResponse;
import com.example.quiz.room.dto.response.RoomResponse;
import com.example.quiz.room.entity.Room;
import com.example.quiz.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Set;

@Mapper(componentModel = "spring")
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
