package com.example.quiz.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter @Setter
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.kakao")
public class KakaoProperties {
    private String clientId;
    private String redirectUri;
    private String authorizationGrantType;
}
