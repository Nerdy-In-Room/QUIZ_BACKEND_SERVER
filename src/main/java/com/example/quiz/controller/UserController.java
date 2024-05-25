package com.example.quiz.controller;

import com.example.quiz.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;


@RestController
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/auth/kakao/callback")
    public ResponseEntity<String> kakaoCallback(String code, HttpServletResponse response) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + userService.kakaoCallback(code, response));

        return ResponseEntity.ok().headers(headers).body("로그인 성공");
    }

    /*
    Redis를 사용하여 Whitelist 방법 고려
     */
}
