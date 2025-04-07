package com.example.quiz.room.controller;

import com.example.quiz.global.config.auth.annotation.user.LoginUser;
import com.example.quiz.room.dto.request.RoomCreateRequest;
import com.example.quiz.room.dto.request.RoomModifyRequest;
import com.example.quiz.room.dto.response.*;
import com.example.quiz.room.service.RoomProducerService;
import com.example.quiz.room.service.RoomService;
import com.example.quiz.user.dto.request.LoginUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final RoomProducerService roomProducerService;

    @PostMapping("/room")
    public String createRoom(RoomCreateRequest roomRequest, @LoginUser LoginUserRequest loginUserRequest) {
        RoomResponse roomResponse = roomProducerService.createRoom(roomRequest, loginUserRequest);

        return "redirect:/room/" + roomResponse.roomId() + "?status=master";
    }

    @GetMapping("/room-list")
    public ModelAndView getRoomList(@RequestParam(name = "page") Optional<Integer> page) {
        int index = page.orElse(1) - 1;
        Page<RoomListResponse> roomListResponses = roomProducerService.roomList(index);
        Map<String, Object> map = new HashMap<>();

        String roomIds = roomListResponses.stream()
                .map(RoomListResponse::roomId).map(String::valueOf).collect(
                Collectors.joining(","));
        map.put("roomList", roomListResponses);
        map.put("roomIds", roomIds);

        return new ModelAndView("index", map);
    }

    @GetMapping("/room/{roomId}")
    public ModelAndView enterRoom(@PathVariable Long roomId,
                                  @LoginUser LoginUserRequest loginUserRequest,
                                  @RequestParam(required = false) String status) {
        RoomEnterResponse roomEnterResponse = roomService.enterRoom(roomId, loginUserRequest, status);

        if (roomEnterResponse.participants().isEmpty()) {
            return new ModelAndView("redirect:/room-list");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("roomInfo", roomEnterResponse);

        return new ModelAndView("room", map);
    }

    @ResponseBody
    @PatchMapping("/room/{roomId}")
    public ResponseEntity<RoomModifyResponse> modifyRoom(@PathVariable Long roomId, RoomModifyRequest request, @LoginUser LoginUserRequest loginUserRequest) {
        RoomModifyResponse roomModifyResponse = roomService.modifyRoom(request, roomId, loginUserRequest);

        return ResponseEntity.ok(roomModifyResponse);
    }
}