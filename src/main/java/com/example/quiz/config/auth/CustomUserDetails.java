package com.example.quiz.config.auth;


import com.example.quiz.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

// Spring Security가 요청을 가로채서 로그인을 진행하고 완료가 되면 UserDetails 타입의 오브젝트를 Spring Security의 고유 세션 저장소에 저장해준다.
public class CustomUserDetails implements UserDetails {
    private User user; // 콤포지션

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    //true : 만료안됨
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    //true : 계정이 잠겨있지 않음
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    //true : 비밀번호 만료 안됨
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    //true : 계정 활성화 되어 있음
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collectors = new ArrayList<>();

        collectors.add(() -> {
            return "ROLE_" + user.getRole(); //ROLE_USER
        });

        return collectors;
    }
}
