package com.example.quiz.controller;

import com.example.quiz.dto.response.CurrentOccupancy;
import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.request.RoomModifyRequest;
import com.example.quiz.dto.room.response.RoomEnterResponse;
import com.example.quiz.dto.room.response.RoomListResponse;
import com.example.quiz.dto.room.response.RoomModifyResponse;
import com.example.quiz.service.RoomProducerService;
import com.example.quiz.service.RoomService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    private final RoomService roomService;
    private final RoomProducerService roomProducerService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping(value = "/room")
    public String createRoom(RoomCreateRequest roomRequest) {
        roomRequest.setMasterEmail("sample@master.com");
        long roomId = roomProducerService.createRoom(roomRequest);

        return "redirect:/room/" + roomId;
    }

    @GetMapping("/room-list")
    public ModelAndView getRoomList(@RequestParam(name = "page") Optional<Integer> page) {
        int index = page.orElse(1) - 1;
        Page<RoomListResponse> roomListResponses = roomProducerService.roomList(index);
        Map<String, Object> map = new HashMap<>();

        String roomIds = roomListResponses.stream().map(RoomListResponse::getRoomId).map(String::valueOf).collect(
                Collectors.joining(","));
        map.put("roomList", roomListResponses);
        map.put("roomIds", roomIds);

        return new ModelAndView("index", map);
    }

    @GetMapping("/room/{roomId}")
    public ModelAndView enterRoom(@PathVariable Long roomId) throws IllegalAccessException {
        RoomEnterResponse roomEnterResponse = roomService.enterRoom(roomId);
        Map<String, Object> map = new HashMap<>();
        map.put("roomInfo", roomEnterResponse);

        return new ModelAndView("room", map);
    }

    @ResponseBody
    @PatchMapping("/room/{roomId}")
    public ResponseEntity<RoomModifyResponse> modifyRoom(@PathVariable Long roomId, RoomModifyRequest request) {
        RoomModifyResponse roomModifyResponse = roomService.modifyRoom(request, roomId);

        return ResponseEntity.ok(roomModifyResponse);
    }

//    @SendTo("/pub/occupancy")
//    @MessageMapping("/updateOccupancy")
//    public List<CurrentOccupancy> sendCurrentOccupancy(@Payload List<Long> currentPageRooms) {
//        log.info("요청 들어옴 : {}", currentPageRooms);
//
//        return roomService.getCurrentOccupancy(currentPageRooms);
//    }
}