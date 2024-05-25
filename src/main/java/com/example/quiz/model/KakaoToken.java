package com.example.quiz.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.security.oauth2.client.provider.kakao")
public class KakaoToken {
    private String tokenUri;
    private String userInfoUri;
}
