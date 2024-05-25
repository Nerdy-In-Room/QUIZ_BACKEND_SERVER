package com.example.quiz.service;

import com.example.quiz.entity.User;
import com.example.quiz.enums.Role;
import com.example.quiz.jwt.JWTUtil;
import com.example.quiz.model.KakaoProfile;
import com.example.quiz.model.KakaoProperties;
import com.example.quiz.model.KakaoToken;
import com.example.quiz.model.OAuthToken;
import com.example.quiz.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final KakaoProperties kakaoProperties;
    private final KakaoToken kakaoToken;

    public UserService(
            UserRepository userRepository,
            BCryptPasswordEncoder encoder,
            KakaoProperties kakaoProperties,
            KakaoToken kakaoToken) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.kakaoProperties = kakaoProperties;
        this.kakaoToken = kakaoToken;
    }

    @Transactional
    public String kakaoCallback(String code, HttpServletResponse response) {
        //카카오로부터 유저 정보 받아오기
        OAuthToken oAuthToken = requestKakaoToken(code);
        KakaoProfile kakaoProfile = requestKakaoProfile(oAuthToken.getAccess_token());

        //로그인 회원가입 단계
        User kakaoUser = User.builder()
                .username(kakaoProfile.getKakao_account().getEmail() + "_" + kakaoProfile.getId())
                .password("")
                .email(kakaoProfile.getKakao_account().getEmail())
                .build();

        //가입자 혹은 비가입자 체크 해서 처리
        userRepository
                .findByUsername(kakaoUser.getUsername())
                .orElseGet(() -> signUp(kakaoUser));

        String accessToken = JWTUtil.generateJWTToken(kakaoUser);
        String refreshToken = JWTUtil.generateRefreshToken(kakaoUser);

        // Cookie 생성
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setMaxAge(1000000); // 만료 시간 설정 (초 단위)
        refreshTokenCookie.setPath("/"); // 모든 경로에서 접근 가능하도록 설정
        // HTTP 응답에 Cookie 추가
        response.addCookie(refreshTokenCookie);

        return accessToken;
        /*
        accessToken은 Header, refreshToken은 Cookie
         */
    }

    private OAuthToken requestKakaoToken(String code) {
        RestTemplate rt = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", kakaoProperties.getAuthorizationGrantType());
        params.add("client_id", kakaoProperties.getClientId());
        params.add("redirect_uri", kakaoProperties.getRedirectUri());
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest =
                new HttpEntity<>(params, httpHeaders);

        ResponseEntity<String> response = rt.exchange(
                kakaoToken.getTokenUri(),
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response.getBody(), OAuthToken.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private KakaoProfile requestKakaoProfile(String accessToken) {
        RestTemplate rt = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer " + accessToken);
        httpHeaders.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> kakaoProfileRequest =
                new HttpEntity<>(httpHeaders);

        ResponseEntity<String> response2 = rt.exchange(
                kakaoToken.getUserInfoUri(),
                HttpMethod.POST,
                kakaoProfileRequest,
                String.class
        );

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response2.getBody(), KakaoProfile.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public User signUp(User user) {
        String rawPassword = user.getPassword();
        String encPassword = encoder.encode(rawPassword);
        user.setRole(Role.USER);
        user.setPassword(encPassword);
        return userRepository.save(user);
    }

    @Transactional
    public void userUpdate(User user) {
        User persistance = userRepository.findById(user.getId()).orElseThrow(() -> {
            return new IllegalArgumentException("회원 찾기 실패");
        });

        String rawPassword = user.getPassword();
        String encPassword = encoder.encode(rawPassword);
        persistance.setPassword(encPassword);
        persistance.setEmail(user.getEmail());

        //회원 수정 함수 종료 시 = 서비스 종료 = 트랜잭션 종료 = commit이 자동으로 수행
        //영속화된 persistance 객체 변화 감지 시 더티 체킹하여 update문을 날려준다.
    }
}
