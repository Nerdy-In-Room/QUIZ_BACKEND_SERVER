package com.example.quiz.controller;


import com.example.quiz.dto.room.request.RoomCreateRequest;
import com.example.quiz.service.RoomProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

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
}
