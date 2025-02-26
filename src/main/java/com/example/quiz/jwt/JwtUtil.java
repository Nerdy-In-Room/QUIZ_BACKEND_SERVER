package com.example.quiz.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.example.quiz.dto.User.LoginUserRequest;
import com.example.quiz.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {
    private static Algorithm algorithm;
    private static final long EXPIRATION_TIME = 1000 * 60 * 60;
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 7;

    @Value("${secret.key}")
    public void setSecretKey(String key) {
        algorithm = Algorithm.HMAC256(key);
    }

    public String generateToken(Long userId, String email, String role) {
        return JWT.create()
                .withSubject("UserDetails")
                .withClaim("userId", userId)
                .withClaim("email", email)
                .withClaim("roles", role)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(algorithm);
    }

    public static String generateRefreshToken(Long userId) {
        return JWT.create()
                .withSubject("RefreshToken")
                .withClaim("userId", userId)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .sign(algorithm);
    }

    public static LoginUserRequest verifyToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT decodedJWT = verifier.verify(token);

        Long userId = decodedJWT.getClaim("userId").asLong();
        String email = decodedJWT.getClaim("email").asString();
        Role role = decodedJWT.getClaim("roles").as(Role.class);

        return new LoginUserRequest(userId, email, role.name());
    }
}