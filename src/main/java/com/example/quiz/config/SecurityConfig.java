package com.example.quiz.config;


import com.example.quiz.jwt.JWTRequestFilter;
import com.example.quiz.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration //빈 등록(IoC 관리)
@EnableWebSecurity // Security 필터가 등록이 된다.
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTUtil jwtUtil;
    @Bean
    public BCryptPasswordEncoder encoderPWD() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .formLogin().disable()
                .authorizeRequests()
                .antMatchers("/hello").authenticated()
                .anyRequest().permitAll()
                .and()
                .addFilterBefore(new JWTRequestFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                /**
                 * LoginFilter가 없어져도 되는 이유 : UsernamePasswordAuthenticationFilter가 안에서 다 해줌
                 */
                .build();
    }
}