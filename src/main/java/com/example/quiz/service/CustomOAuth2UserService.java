package com.example.quiz.service;

import com.example.quiz.entity.user.CustomOAuth2User;
import com.example.quiz.entity.user.User;
import com.example.quiz.enums.Role;
import com.example.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        String kakaoId = String.valueOf(attributes.get("id"));
        String email = (String) kakaoAccount.get("email");
        String username = email + "_" + kakaoId;

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(new User(username, email, Role.USER)));

        return new CustomOAuth2User(user, attributes, null, null);
    }
}
