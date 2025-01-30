package com.example.quiz.controller;

import com.example.quiz.model.KakaoProperties;
import com.example.quiz.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final KakaoProperties kakaoProperties;

    @GetMapping("/oauth/kakao")
    public ResponseEntity<Object> redirectToKakao() {
        HttpHeaders headers = new HttpHeaders();

        String kakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize" +
                "?client_id=" + kakaoProperties.getClientId() +
                "&redirect_uri=" + kakaoProperties.getRedirectUri() +
                "&response_type=code";

        headers.setLocation(URI.create(kakaoAuthUrl));

        return new ResponseEntity<>(headers, HttpStatus.PERMANENT_REDIRECT);
    }

    @GetMapping("/auth/kakao/callback")
    public ResponseEntity<String> kakaoCallback(@RequestParam("code") String code, HttpServletResponse response) {
        userService.kakaoCallback(code, response);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/room-list"));

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
