package com.example.quiz.controller;


import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.dto.room.response.RoomListResponse;
import com.example.quiz.service.RoomProducerService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class RoomController {

    private final RoomProducerService roomProducerService;

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
        map.put("roomList", roomListResponses);

        return new ModelAndView("index", map);
    }
}
