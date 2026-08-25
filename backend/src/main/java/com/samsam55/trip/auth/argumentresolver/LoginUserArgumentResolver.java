package com.samsam55.trip.auth.argumentresolver;

import com.samsam55.trip.auth.annotation.Login;
import com.samsam55.trip.auth.exception.AuthErrorType;
import com.samsam55.trip.auth.service.AuthService;
import com.samsam55.trip.global.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(Login.class)) {
            return false;
        }

        Class<?> parameterType = parameter.getParameterType();
        return parameterType == Long.class || parameterType == long.class;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new ApplicationException(AuthErrorType.LOGIN_REQUIRED);
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ApplicationException(AuthErrorType.LOGIN_REQUIRED);
        }

        Object sessionUserId = session.getAttribute(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE);
        if (!(sessionUserId instanceof Number number)) {
            throw new ApplicationException(AuthErrorType.LOGIN_REQUIRED);
        }

        Long userId = number.longValue();
        if (parameter.getParameterType() == long.class) {
            return userId.longValue();
        }
        return userId;
    }
}
