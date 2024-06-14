package com.example.quiz.controller;

import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.request.RoomModifyRequest;
import com.example.quiz.dto.room.response.RoomEnterResponse;
import com.example.quiz.dto.room.response.RoomListResponse;
import com.example.quiz.dto.room.response.RoomModifyResponse;
import com.example.quiz.service.RoomProducerService;
import com.example.quiz.service.RoomService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);
    private final RoomProducerService roomProducerService;
    private final RoomService roomService;

    @PostMapping(value = "/room")
    public ModelAndView createRoom(RoomCreateRequest roomRequest) throws IllegalAccessException {
        roomRequest.setMasterEmail("sample@master.com");
        RoomEnterResponse roomEnterResponse = roomProducerService.createRoom(roomRequest);
        Map<String, Object> map = new HashMap<>();
        map.put("roomInfo", roomEnterResponse);

        return new ModelAndView("room", map);
    }

    @GetMapping("/room-list")
    public ModelAndView getRoomList(@RequestParam(name = "page") Optional<Integer> page) {
        int index = page.orElse(1) - 1;
        Page<RoomListResponse> roomListResponses = roomProducerService.roomList(index);
        Map<String, Object> map = new HashMap<>();
        map.put("roomList", roomListResponses);

        return new ModelAndView("index", map);
    }

    @GetMapping("/room/{roomId}")
    public ModelAndView enterRoom(@PathVariable Long roomId) throws IllegalAccessException {
        log.info("room Id: {}", roomId);
        RoomEnterResponse roomEnterResponse = roomService.enterRoom(roomId);
        Map<String, Object> map = new HashMap<>();
        map.put("roomInfo", roomEnterResponse);

        return new ModelAndView("room", map);
    }

    @PatchMapping("/room/{roomId}")
    @ResponseBody
    public ResponseEntity<RoomModifyResponse> modifyRoom(@PathVariable Long roomId, RoomModifyRequest request) {
        RoomModifyResponse roomModifyResponse = roomService.modifyRoom(request, roomId);

        return ResponseEntity.ok(roomModifyResponse);
    }
}