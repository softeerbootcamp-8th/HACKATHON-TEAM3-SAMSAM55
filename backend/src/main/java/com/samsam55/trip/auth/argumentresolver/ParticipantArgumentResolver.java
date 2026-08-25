package com.samsam55.trip.auth.argumentresolver;

import com.samsam55.trip.auth.annotation.CurrentParticipant;
import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.auth.service.ParticipantSessionResolver;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.exception.TripErrorType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentParticipant}가 붙은 파라미터에 현재 요청의 참여자 정보를 주입한다.
 * 실제 세션/쿠키 조회는 {@link ParticipantSessionResolver}가 담당한다.
 */
@Component
@RequiredArgsConstructor
public class ParticipantArgumentResolver implements HandlerMethodArgumentResolver {

    private final ParticipantSessionResolver participantSessionResolver;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentParticipant.class)
                && parameter.getParameterType() == ParticipantPrincipal.class;
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
            throw new ApplicationException(TripErrorType.PARTICIPANT_LOGIN_REQUIRED);
        }

        return participantSessionResolver.resolve(request)
                .orElseThrow(() -> new ApplicationException(TripErrorType.PARTICIPANT_LOGIN_REQUIRED));
    }
}
