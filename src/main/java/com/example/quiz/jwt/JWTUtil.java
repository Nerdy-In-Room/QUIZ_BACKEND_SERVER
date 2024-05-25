package com.example.quiz.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.quiz.entity.User;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JWTUtil {

    private static final String JWT_SECRET = "secret_key";

    public String getUsername(String token) {
        return JWT.decode(token).getClaim("username").asString();
    }

//    public Role getRole(String token) {
//        String role = JWT.decode(token).getClaim("role").asString();
//        return Role.valueOf(role);
//    }
//
//    public String getEmail(String token) {
//        return JWT.decode(token).getClaim("email").asString();
//    }

    public Boolean isExpired(String token) {
        return JWT.decode(token).getExpiresAt().before(new Date());
    }

    public static String generateJWTToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 100000);

        return JWT.create()
                .withSubject(user.getUsername())
                .withClaim("id", user.getId())
                .withClaim("username", user.getUsername())
                .withClaim("email", user.getEmail())
                .withExpiresAt(expiryDate)
                .sign(Algorithm.HMAC512(JWT_SECRET));
    }

    public static String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 1000000);

        return JWT.create()
                .withSubject(user.getUsername())
                .withClaim("id", user.getId())
                .withClaim("username", user.getUsername())
                .withClaim("email", user.getEmail())
                .withExpiresAt(expiryDate)
                .sign(Algorithm.HMAC512(JWT_SECRET));
    }

    public static DecodedJWT verifyJWTToken(String token) {
        return JWT.require(Algorithm.HMAC512(JWT_SECRET))
                .build()
                .verify(token);
    }
}
