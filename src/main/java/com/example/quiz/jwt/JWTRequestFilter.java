package com.example.quiz.jwt;


import com.example.quiz.config.auth.CustomUserDetails;
import com.example.quiz.entity.User;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/*
Filter부분을 고도화 : try-catch 문으로 감싸고 catch로 예외를 handling
finally 구문을 통한 SecurityContext 초기화
- Thread Pool을 이용하기에, 인증 안된 사용자가 아무것도 없이 접근한 경우 다른 사람의 인증 정보를 사용할 수 있기에 사용한다.
 */

@RequiredArgsConstructor
public class JWTRequestFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);

            return;
        }

        String token = authorization.split(" ")[1];

        if (jwtUtil.isExpired(token)) {
            filterChain.doFilter(request, response);

            return;
        }

        String username = jwtUtil.getUsername(token);

        User user = new User();
        user.setUsername(username);

        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails.getUsername(), customUserDetails.getPassword(), customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
