package com.example.quiz.room.controller;

import com.example.quiz.game.model.InGameUser;
import com.example.quiz.global.type.Role;
import com.example.quiz.helper.MockLoginUserArgumentResolver;
import com.example.quiz.room.dto.request.RoomModifyRequest;
import com.example.quiz.room.dto.response.RoomEnterResponse;
import com.example.quiz.room.dto.response.RoomListResponse;
import com.example.quiz.room.dto.response.RoomModifyResponse;
import com.example.quiz.room.dto.response.RoomResponse;
import com.example.quiz.room.service.RoomProducerService;
import com.example.quiz.room.service.RoomService;
import com.example.quiz.user.dto.request.LoginUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RoomController.class)
class RoomControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RoomProducerService roomProducerService;
    @MockBean
    private RoomService roomService;

    @BeforeEach
    void setup() {
        RoomController roomController = new RoomController(roomService, roomProducerService);
        mockMvc = MockMvcBuilders.standaloneSetup(roomController)
                .setCustomArgumentResolvers(new MockLoginUserArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("방을 만든다.")
    void createRoom() throws Exception {
        // given
        String uuid = "test-uuid";
        RoomResponse response = new RoomResponse(123L, "TestRoom", 3L, 8, 5, 1);

        given(roomProducerService.createRoom(any(), any())).willReturn(response);

        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.post("/room")
                        .param("title", "TestRoom")
                        .param("topicId", "1")
                        .param("maxPeople", "8")
                        .param("quizCount", "5")
                        .param("uuid", uuid))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/room/123?status=master"));
    }

    @Test
    @DisplayName("LoginUser 정보가 없을 경우 401 또는 400 처리한다")
    void createRoomNoLoginUser(@Autowired WebApplicationContext context) throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();

        // give
        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.post("/room")
                        .param("title", "TestRoom")
                        .param("topicId", "1")
                        .param("maxPeople", "8")
                        .param("quizCount", "5")
                        .param("uuid", "uuid-123"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("room list를 반환한다.")
    void getRoomList() throws Exception {
        // given
        List<RoomListResponse> roomList = List.of(
                new RoomListResponse(1L, "room 1", 1L, 8, 1, 1),
                new RoomListResponse(2L, "room 2", 2L, 5, 1, 1)
        );
        Page<RoomListResponse> pageResult = new PageImpl<>(roomList);

        given(roomProducerService.roomList(anyInt())).willReturn(pageResult);

        // when
        // then
        mockMvc.perform(get("/room-list").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("roomList"))
                .andExpect(model().attributeExists("roomIds"))
                .andExpect(model().attribute("roomIds", "1,2"));
    }

    @Test
    @DisplayName("방에 입장한다")
    void enterRoom() throws Exception {
        // given
        Long roomId = 1L;
        InGameUser inGameUser = new InGameUser(1L, 1L, "tset", Role.USER, false);
        Set<InGameUser> set = new HashSet<>();
        set.add(inGameUser);
        RoomEnterResponse mockResponse = new RoomEnterResponse(roomId, "test room", 1L, 8, 5, false, Role.USER, inGameUser, set);

        given(roomService.enterRoom(eq(roomId), any())).willReturn(mockResponse);

        // when
        // then
        mockMvc.perform(get("/room/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(view().name("room"))
                .andExpect(model().attributeExists("roomInfo"));
    }

    @Test
    @DisplayName("입장할려는 방이 빈방이면 room list로 리다이렉트한다.")
    void enterRoomNoPeople() throws Exception {
        // given
        Long roomId = 1L;
        InGameUser inGameUser = new InGameUser(1L, 1L, "tset", Role.USER, false);
        Set<InGameUser> set = new HashSet<>();
        RoomEnterResponse mockResponse = new RoomEnterResponse(roomId, "test room", 1L, 8, 5, false, Role.USER, inGameUser, set);

        given(roomService.enterRoom(eq(roomId), any())).willReturn(mockResponse);

        // when
        // then
        mockMvc.perform(get("/room/{roomId}", roomId))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("로그인하지 않은 유저가 입장할려고 하면 예외를 발생시킨다.")
    void enterRoomNoLoginUser(@Autowired WebApplicationContext context) throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();

        // give
        Long roomId = 1L;

        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.get("/room/{roomId}", roomId))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("방 정보 수정 요청을 성공하면 수정된 정보가 JSON으로 반환된다")
    void modifyRoomInfo() throws Exception {
        // given
        Long roomId = 1L;
        RoomModifyResponse mockResponse = new RoomModifyResponse("modify room", 2L, 6, 8);

        given(roomService.modifyRoom(any(RoomModifyRequest.class), eq(roomId), any(LoginUserRequest.class)))
                .willReturn(mockResponse);

        // when
        // then
        mockMvc.perform(patch("/room/{roomId}", roomId)
                        .param("roomName", "modify room")
                        .param("topicId", "2")
                        .param("maxPeople", "6")
                        .param("quizCount", "8"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.roomName").value("modify room"))
                .andExpect(jsonPath("$.topicId").value(2))
                .andExpect(jsonPath("$.maxPeople").value(6))
                .andExpect(jsonPath("$.quizCount").value(8));
    }

    @Test
    @DisplayName("로그인 정보가 없는 유저가 방 수정 요청을 하면 4xx 예외를 발생시킨다.")
    void modifyRoomInfoNoLoginUser(@Autowired WebApplicationContext context) throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();

        // give
        Long roomId = 1L;

        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.patch("/room/{roomId}", roomId))
                .andExpect(status().is4xxClientError());
    }
}