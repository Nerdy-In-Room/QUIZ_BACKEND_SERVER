package com.example.quiz.config.stompConfig;

import com.example.quiz.dto.User.LoginUserRequest;
import com.example.quiz.jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.WebUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            String requestURI = httpServletRequest.getRequestURI();

            if (requestURI.startsWith("/updateOccupancy")) {
                return true;
            }

            Cookie jwtToken = WebUtils.getCookie(httpServletRequest, "accessToken");

            if (jwtToken == null) {
                throw new IllegalArgumentException("JWT token is missing");
            }

            String decodeToken = URLDecoder.decode(jwtToken.getValue(), StandardCharsets.UTF_8).split(" ")[1];
            LoginUserRequest loginUser = JwtUtil.verifyToken(decodeToken);
            attributes.put("loginUser", loginUser);
        }

        String query = request.getURI().getQuery();
        if (query != null && query.contains("state")) {
            String state = query.split("state=")[1];

            attributes.put("state", state);
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
