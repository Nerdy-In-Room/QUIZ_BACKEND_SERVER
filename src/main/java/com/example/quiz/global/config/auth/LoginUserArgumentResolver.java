package com.example.quiz.global.config.auth;

import com.example.quiz.global.config.auth.annotation.user.LoginUser;
import com.example.quiz.user.dto.request.LoginUserRequest;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean isAnnotation = parameter.hasParameterAnnotation(LoginUser.class);
        boolean isLogginedMember = LoginUserRequest.class.equals(parameter.getParameterType());
        return isLogginedMember && isAnnotation;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("error authentication");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUserRequest) {

            return principal;
        }

        throw new IllegalStateException("Authentication Principal is not of type LoggedInMember");
    }
}
